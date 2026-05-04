package com.flags.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flags.dto.EvaluateRequest;
import com.flags.dto.EvaluateResponse;
import com.flags.model.FeatureFlag;
import com.flags.model.FlagEvaluationLog;
import com.flags.repository.FeatureFlagRepository;
import com.flags.repository.FlagEvaluationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * FlagEvaluationService — The SDK endpoint: "Should this feature be ON for this user?"
 *
 * This is the core of a feature flag system. Your application calls /api/flags/evaluate
 * and gets back true/false plus a REASON explaining WHY it was enabled or disabled.
 *
 * EVALUATION WATERFALL (checked in priority order):
 *   1. FLAG_NOT_FOUND    → flag key doesn't exist in DB → false
 *   2. FLAG_DISABLED     → flag's global enabled toggle is off → false
 *   3. ENV_DISABLED      → flag is off for this specific environment (e.g., disabled in "production") → false
 *   4. TARGETED_USER     → userId is in the explicit allow-list → always true
 *   5. FULL_ROLLOUT      → rolloutPercentage = 100 → always true for everyone
 *   6. ZERO_ROLLOUT      → rolloutPercentage = 0 → false for everyone
 *   7. PERCENTAGE_ROLLOUT → hash-based bucketing → deterministic true/false for this user
 *   8. NO_USER_ID        → percentage bucketing requires a userId, none provided → false
 *
 * THE BUCKETING ALGORITHM (step 7 — the clever part):
 *   bucket = Math.abs((flagKey + userId).hashCode()) % 100
 *   enabled = bucket < rolloutPercentage
 *
 *   Example: rolloutPercentage=30, flagKey="new-checkout", userId="user-abc"
 *     hash("new-checkoutuser-abc") → some integer → mod 100 → say 17
 *     17 < 30 → ENABLED for this user
 *     The same userId always gets the same bucket number, so the result is STICKY:
 *     user-abc always sees new-checkout enabled, forever, regardless of when they check.
 *
 *   WHY CONCATENATE flagKey + userId?
 *   If we only used userId, then a user who's in bucket 17 would get ALL flags that cover bucket 17.
 *   Including the flagKey in the hash makes each flag's bucketing independent.
 *   user-abc might be enabled for "new-checkout" but disabled for "ai-recommendations".
 *
 * Every evaluation is logged to flag_evaluation_logs table for analytics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlagEvaluationService {

    private final FeatureFlagRepository flagRepository;
    private final FlagEvaluationLogRepository logRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Main evaluation method — the drop-in SDK your apps call.
     * Returns result + reason so callers can debug why a flag is on/off.
     */
    public EvaluateResponse evaluate(EvaluateRequest request) {
        String key = request.getFlagKey();
        String userId = request.getUserId();
        String env = request.getEnvironment();

        // Step 1: Flag must exist
        FeatureFlag flag = flagRepository.findByFlagKey(key).orElse(null);
        if (flag == null) {
            return log(key, userId, env, false, "FLAG_NOT_FOUND");
        }

        // Step 2: Global kill switch — if flag is disabled, nothing else matters
        if (!flag.isEnabled()) {
            return log(key, userId, env, false, "FLAG_DISABLED");
        }

        // Step 3: Environment-specific toggle
        // The "environments" field is JSON like: {"production": true, "staging": false}
        // This lets you have a flag ON in staging but OFF in production
        try {
            var envMap = objectMapper.readTree(flag.getEnvironments());
            if (envMap.has(env) && !envMap.get(env).asBoolean()) {
                return log(key, userId, env, false, "ENV_DISABLED");
            }
        } catch (Exception ignored) {
            // If environments field is malformed or empty, skip this check
        }

        // Step 4: Explicit targeting — specific users always get the feature regardless of %
        // Useful for internal beta testers, VIP users, QA accounts, etc.
        if (userId != null && isTargetedUser(flag.getTargetedUserIds(), userId)) {
            return log(key, userId, env, true, "TARGETED_USER");
        }

        // Step 5: Full rollout shortcut — no need to hash if it's 100%
        if (flag.getRolloutPercentage() >= 100) {
            return log(key, userId, env, true, "FULL_ROLLOUT");
        }

        // Step 6: Zero rollout shortcut — no need to hash if it's 0%
        if (flag.getRolloutPercentage() <= 0) {
            return log(key, userId, env, false, "ZERO_ROLLOUT");
        }

        // Step 7: Consistent percentage bucketing
        // This is the LaunchDarkly-style algorithm — same user always gets same result
        if (userId != null) {
            // Combine flagKey + userId so each flag has independent bucketing per user
            int bucket = Math.abs((key + userId).hashCode()) % 100;  // 0–99
            boolean result = bucket < flag.getRolloutPercentage();    // e.g. bucket=17, rollout=30 → true
            return log(key, userId, env, result, "PERCENTAGE_ROLLOUT");
        }

        // Step 8: Can't do percentage bucketing without a userId
        return log(key, userId, env, false, "NO_USER_ID");
    }

    /**
     * Parses the targetedUserIds JSON array and checks if userId is in it.
     * Format: ["user-vip-001", "user-beta-001", "internal-qa"]
     * Returns false on any parse error (defensive — malformed JSON shouldn't crash evaluation).
     */
    private boolean isTargetedUser(String targetedJson, String userId) {
        try {
            List<String> targeted = objectMapper.readValue(targetedJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            return targeted.contains(userId);
        } catch (Exception e) {
            return false;  // treat malformed targeting config as "no targets"
        }
    }

    /**
     * Saves the evaluation to the audit log and returns the response.
     * Logging is best-effort — a DB write failure never blocks the evaluation result.
     */
    private EvaluateResponse log(String key, String userId, String env, boolean result, String reason) {
        try {
            logRepository.save(FlagEvaluationLog.builder()
                    .flagKey(key).userId(userId).environment(env)
                    .result(result).reason(reason).build());
        } catch (Exception e) {
            log.debug("Failed to save evaluation log: {}", e.getMessage());
        }
        return new EvaluateResponse(key, result, reason, userId, env);
    }
}
