package com.buffer.util;

import java.util.UUID;

/**
 * Centralized ID generation utility for all entities.
 * Provides consistent and unique identifier generation across the application.
 */
public class IdGenerator {
    
    /**
     * Generate a unique channel ID
     * @return A unique channel identifier
     */
    public static String generateChannelId() {
        return "channel_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
    
    /**
     * Generate a unique content idea ID
     * @return A unique idea identifier
     */
    public static String generateIdeaId() {
        return "idea_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
    
    /**
     * Generate a chat/conversation ID (used by OpenAI service)
     * @return A unique chat identifier
     */
    public static String generateChatId() {
        return UUID.randomUUID().toString();
    }
}
