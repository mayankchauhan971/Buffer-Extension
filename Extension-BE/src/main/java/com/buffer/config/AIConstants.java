package com.buffer.config;

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
    public static final String BUSINESS_CONTEXT = "I have a small startup that helps small business collect, manage and analyze their reviews";
    public static final String TARGET_AUDIENCE = "mostly small business owners";

    // Defaults
    public static final List<String> DEFAULT_CHANNELS = Collections.unmodifiableList(
            Arrays.asList("Instagram", "X", "LinkedIn")
    );

    // JSON Schema configuration
    public static final String SCHEMA_NAME = "content_ideas_schema";
    public static final boolean STRICT_SCHEMA = true;
    public static final int IDEA_MIN_ITEMS = 1;
    public static final int IDEA_MAX_ITEMS = 3;
}


