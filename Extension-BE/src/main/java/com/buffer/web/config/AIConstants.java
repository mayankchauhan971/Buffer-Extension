package com.buffer.web.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class AIConstants {
    private AIConstants() {}

    // OpenAI API configuration
    public static final String OPENAI_BASE_URL = "https://api.openai.com/v1";
    public static final String OPENAI_MODEL = "gpt-4o-mini";
    public static final double OPENAI_TEMPERATURE = 0.7;

    // Retry configuration
    public static final int RETRY_MAX_ATTEMPTS = 3;
    public static final int RETRY_INITIAL_DELAY_SECONDS = 1;
    public static final int RETRY_MAX_BACKOFF_SECONDS = 5;

    // Business context for prompt
    public static final String BUSINESS_CONTEXT = "I have a small startup that helps small business collect, " +
            "manage and analyze their reviews";
    public static final String TARGET_AUDIENCE = "mostly small business owners";

    // Defaults
    public static final List<String> DEFAULT_CHANNELS = Collections.unmodifiableList(
            Arrays.asList("INSTAGRAM", "X", "LINKEDIN")
    );

    // JSON Schema configuration
    public static final String SCHEMA_NAME = "content_ideas_schema";
    public static final boolean STRICT_SCHEMA = true;
    public static final int IDEA_MIN_ITEMS = 1;
    public static final int IDEA_MAX_ITEMS = 2;
    
    // Content limits
    public static final int MAX_CONTENT_LENGTH = 50000;
    public static final int TRUNCATED_CONTENT_LENGTH = 30000; // Reduced content length for retry
    
    // Schema enum values
    public static final String[] STATUS_VALUES = {"SUCCESS", "FAILURE"};
    
    // System prompt template
    public static final String SYSTEM_PROMPT_TEMPLATE = 
        "You are an expert social media strategist and content ideation assistant.\n" +
        "You need to first generate a concise summary of the content answering what is the main idea of the content. Keep it super short and concise. It should be 2-3 sentences." +
        "Then your task is to generate 3 unique, actionable content ideas per channel, 3 for each of the following social media channels: {CHANNELS}. \n\n" +
        "Each idea should:\n" +
        "- Be highly specific and detailed about the idea so that generating content from idea is easy.\n" +
        "- Be tailored to the selected platform's format, audience behavior, and content trends keeping in mind what works and what not\n" +
        "- Reflect the business's voice, tone, and business context.\n\n" +
        "For each idea, provide:\n" +
        "1. A clear and creative content idea\n" +
        "2. Why it would perform well on the specific platform (platform rationale), why would users love it or find it useful\n" +
        "3. 2-3 benefits of posting it (pros)\n" +
        "4. 1-2 potential limitations (cons)\n\n" +
        "Make sure ideas are deeply personalized and practical, avoiding vague or generic suggestions.\n" +
        "Keep the tone helpful and professional.\n\n" +
        "Business context: {BUSINESS_CONTEXT}\n" +
        "Target audience: {TARGET_AUDIENCE}\n\n" +
        "Return your response as valid JSON with the following structure:\n" +
        "{\n" +
        "  \"status\": \"SUCCESS\",\n" +
        "  \"summary\": \"brief summary of the content\",\n" +
        "  \"channels\": {\n" +
        "    \"INSTAGRAM\": [array of idea objects],\n" +
        "    \"X\": [array of idea objects],\n" +
        "    \"LINKEDIN\": [array of idea objects]\n" +
        "  }\n" +
        "}\n" +
        "Each idea object should contain: idea, rationale, pros, cons.\n\n";
    
    /**
     * Build complete system prompt with dynamic values
     */
    public static String buildSystemPrompt(String channels, String businessContext, String targetAudience) {
        return SYSTEM_PROMPT_TEMPLATE
            .replace("{CHANNELS}", channels)
            .replace("{BUSINESS_CONTEXT}", businessContext)
            .replace("{TARGET_AUDIENCE}", targetAudience);
    }
}


