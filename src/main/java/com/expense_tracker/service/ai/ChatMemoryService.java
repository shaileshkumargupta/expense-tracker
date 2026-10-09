package com.expense_tracker.service.ai;

import com.expense_tracker.dto.ai.ChatMessageDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class ChatMemoryService {

    private static final String CHAT_HISTORY_PREFIX = "chat:history:";
    private static final Duration SLIDING_WINDOW_TTL = Duration.ofMinutes(30);
    private static final long MAX_MESSAGES_WINDOW = 20; // 10 user-assistant conversation turns

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public ChatMemoryService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    private String buildKey(String userIdentifier) {
        return CHAT_HISTORY_PREFIX + userIdentifier;
    }

    /**
     * Appends a message to the user's sliding session memory and refreshes the 30-minute TTL.
     */
    public void appendMessage(String userIdentifier, ChatMessageDto message) {
        if (userIdentifier == null || message == null) {
            return;
        }

        String key = buildKey(userIdentifier);
        try {
            redisTemplate.opsForList().rightPush(key, message);
            // Retain only the last MAX_MESSAGES_WINDOW elements
            redisTemplate.opsForList().trim(key, -MAX_MESSAGES_WINDOW, -1);
            // Refresh 30-minute sliding window TTL
            redisTemplate.expire(key, SLIDING_WINDOW_TTL);
            log.debug("Appended message to Redis chat memory for user: {}, role: {}", userIdentifier, message.getRole());
        } catch (Exception e) {
            log.error("Failed to append chat message to Redis memory for user {}: {}", userIdentifier, e.getMessage());
        }
    }

    /**
     * Retrieves the chronological sliding conversation history for the user.
     */
    public List<ChatMessageDto> getHistory(String userIdentifier) {
        if (userIdentifier == null) {
            return Collections.emptyList();
        }

        String key = buildKey(userIdentifier);
        try {
            List<Object> rawList = redisTemplate.opsForList().range(key, 0, -1);
            if (rawList == null || rawList.isEmpty()) {
                return Collections.emptyList();
            }

            List<ChatMessageDto> history = new ArrayList<>();
            for (Object obj : rawList) {
                if (obj instanceof ChatMessageDto) {
                    history.add((ChatMessageDto) obj);
                } else {
                    ChatMessageDto converted = objectMapper.convertValue(obj, ChatMessageDto.class);
                    history.add(converted);
                }
            }

            // Touch TTL on read access as well to maintain session continuity
            redisTemplate.expire(key, SLIDING_WINDOW_TTL);
            return history;
        } catch (Exception e) {
            log.error("Failed to read chat history from Redis for user {}: {}", userIdentifier, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Clears all conversational memory for the user.
     */
    public void clearHistory(String userIdentifier) {
        if (userIdentifier == null) {
            return;
        }
        String key = buildKey(userIdentifier);
        try {
            redisTemplate.delete(key);
            log.info("Cleared Redis chat memory for user: {}", userIdentifier);
        } catch (Exception e) {
            log.error("Failed to clear chat history from Redis for user {}: {}", userIdentifier, e.getMessage());
        }
    }
}
