package com.flags.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * FlagUpdateSubscriber — The bridge between Redis pub/sub and WebSocket.
 *
 * This is the second half of the real-time update pipeline:
 *   FeatureFlagService publishes to Redis channel "flag.updates"
 *   → Redis delivers to ALL subscribed backend instances
 *   → THIS CLASS receives it on each instance
 *   → Forwards the message to WebSocket topic /topic/flags
 *   → Every connected browser tab receives it and re-renders
 *
 * WHY THIS CLASS EXISTS (instead of WebSocket broadcasting directly):
 * If you have 3 backend instances, only instance #2 handled the toggle HTTP request.
 * Instances #1 and #3 don't know about it — their WebSocket clients would be stale.
 * By going through Redis, ALL instances receive the notification and push to their own clients.
 * This makes the system horizontally scalable — add more backend instances without breaking live updates.
 *
 * HOW IT'S WIRED UP:
 * See RedisConfig.java — it creates a RedisMessageListenerContainer that registers this class
 * as the listener for the "flag.updates" channel. Spring wires this automatically.
 *
 * WHAT THE PAYLOAD LOOKS LIKE:
 * {"action":"TOGGLED","flagKey":"new-checkout-flow","enabled":true,"id":"abc-123-..."}
 * The React frontend receives this, sees the flagKey, and refreshes that specific flag card.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlagUpdateSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;  // Spring WebSocket broadcaster

    /**
     * Called by the Redis listener container when a message arrives on "flag.updates".
     *
     * message.getBody() contains the raw bytes of the JSON string published by FeatureFlagService.
     * We convert to String and forward it directly to the WebSocket topic.
     * No parsing needed here — React handles the JSON on the frontend.
     *
     * pattern is the channel pattern that matched (not used here since we have a direct channel).
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String payload = new String(message.getBody());
        log.debug("Redis flag update received: {}", payload);
        // Push to ALL connected WebSocket clients subscribed to /topic/flags
        // This happens on every backend instance simultaneously — that's the magic
        messagingTemplate.convertAndSend("/topic/flags", payload);
    }
}
