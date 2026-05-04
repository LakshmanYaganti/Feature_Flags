package com.flags.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flags.dto.FlagRequest;
import com.flags.model.FeatureFlag;
import com.flags.repository.FeatureFlagRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * FeatureFlagService — CRUD for flags, plus real-time broadcasting via Redis pub/sub.
 *
 * THE REAL-TIME UPDATE PROBLEM:
 * When you toggle a flag in the dashboard, you want ALL connected browser tabs to update instantly.
 * You could use a WebSocket broadcast directly, but that only works for the server instance
 * that handled the HTTP request.
 *
 * In production, you might have 3 backend instances behind a load balancer.
 * The toggle might hit instance #2, but browsers might be connected via WebSocket to instance #1 or #3.
 * Those instances wouldn't know about the change.
 *
 * THE REDIS PUB/SUB SOLUTION:
 * Every instance subscribes to the same Redis channel ("flag.updates").
 * When any instance changes a flag, it publishes to Redis.
 * Redis delivers the message to ALL subscriber instances.
 * Each instance then broadcasts to its own connected WebSocket clients.
 * Result: all browsers update regardless of which server they're connected to, in <100ms.
 *
 * FLOW:
 * HTTP PATCH /flags/{id}/toggle
 *   → toggleFlag() saves to PostgreSQL
 *   → publishFlagUpdate() sends JSON to Redis channel "flag.updates"
 *   → FlagUpdateSubscriber.onMessage() (on all instances) receives it
 *   → broadcasts to /topic/flags WebSocket topic
 *   → React dashboard updates in all open browser tabs ⚡
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureFlagService {

    private final FeatureFlagRepository flagRepository;
    private final RedisTemplate<String, String> redisTemplate;  // used to publish to Redis
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Channel name comes from application.yml: redis.channel.flag-updates = "flag.updates"
    @Value("${redis.channel.flag-updates}")
    private String flagUpdateChannel;

    public List<FeatureFlag> getAllFlags() {
        return flagRepository.findAll();
    }

    public FeatureFlag getFlagByKey(String key) {
        return flagRepository.findByFlagKey(key)
                .orElseThrow(() -> new EntityNotFoundException("Flag not found: " + key));
    }

    /**
     * Creates a new feature flag and immediately notifies all connected dashboards.
     * Flag keys must be unique (e.g., "new-checkout-flow", "ai-recommendations").
     */
    @Transactional
    public FeatureFlag createFlag(FlagRequest request) {
        if (flagRepository.existsByFlagKey(request.getFlagKey())) {
            throw new IllegalArgumentException("Flag key already exists: " + request.getFlagKey());
        }
        FeatureFlag flag = FeatureFlag.builder()
                .flagKey(request.getFlagKey())
                .name(request.getName())
                .description(request.getDescription())
                .enabled(request.isEnabled())
                .rolloutPercentage(request.getRolloutPercentage())
                .targetedUserIds(request.getTargetedUserIds())
                .environments(request.getEnvironments())
                .tags(request.getTags())
                .build();
        flag = flagRepository.save(flag);
        publishFlagUpdate(flag, "CREATED");  // tell all dashboard tabs a new flag appeared
        return flag;
    }

    @Transactional
    public FeatureFlag updateFlag(UUID id, FlagRequest request) {
        FeatureFlag flag = flagRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Flag not found: " + id));
        flag.setName(request.getName());
        flag.setDescription(request.getDescription());
        flag.setEnabled(request.isEnabled());
        flag.setRolloutPercentage(request.getRolloutPercentage());
        flag.setTargetedUserIds(request.getTargetedUserIds());
        flag.setEnvironments(request.getEnvironments());
        flag.setTags(request.getTags());
        flag = flagRepository.save(flag);
        publishFlagUpdate(flag, "UPDATED");  // real-time sync to all tabs
        return flag;
    }

    /**
     * Flips the flag's enabled state (ON→OFF or OFF→ON).
     * This is the most-called method — it's what the toggle switch in the UI hits.
     * After saving, publishes to Redis so ALL browser tabs update without a refresh.
     */
    @Transactional
    public FeatureFlag toggleFlag(UUID id) {
        FeatureFlag flag = flagRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Flag not found: " + id));
        flag.setEnabled(!flag.isEnabled());  // flip
        flag = flagRepository.save(flag);
        publishFlagUpdate(flag, "TOGGLED");  // this triggers the <100ms cross-tab update
        return flag;
    }

    @Transactional
    public void deleteFlag(UUID id) {
        FeatureFlag flag = flagRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Flag not found: " + id));
        flagRepository.delete(flag);
        publishFlagUpdate(flag, "DELETED");  // removes the flag card from all dashboards
    }

    /**
     * Publishes a flag change notification to Redis pub/sub channel.
     *
     * The message is a JSON object with action, flagKey, enabled status, and id.
     * FlagUpdateSubscriber.onMessage() receives this and forwards it to WebSocket /topic/flags.
     * React's useWebSocket hook listens there and refreshes the flag list.
     *
     * If Redis is down, we log the error but don't throw — the DB change still succeeded.
     * The dashboard will sync on next page load even if the real-time push fails.
     */
    private void publishFlagUpdate(FeatureFlag flag, String action) {
        try {
            String message = objectMapper.writeValueAsString(
                    java.util.Map.of(
                            "action", action,
                            "flagKey", flag.getFlagKey(),
                            "enabled", flag.isEnabled(),
                            "id", flag.getId().toString()
                    ));
            redisTemplate.convertAndSend(flagUpdateChannel, message);  // publish to Redis
            log.info("Published flag update: {} → {}", action, flag.getFlagKey());
        } catch (Exception e) {
            log.error("Failed to publish flag update: {}", e.getMessage());
            // Don't rethrow — the flag was already saved to DB, real-time sync is best-effort
        }
    }
}
