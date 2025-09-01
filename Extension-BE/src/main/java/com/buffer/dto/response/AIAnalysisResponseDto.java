package com.buffer.dto.response;

import lombok.Data;
import java.util.Map;

/**
 * AI Analysis Response DTO
 *
 * DTO representing the structure of AI response from OpenAI service.
 * Contains field name constants to avoid magic strings and ensure consistency
 * when parsing AI-generated JSON responses.
 */
@Data
public class AIAnalysisResponseDto {
    
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_SUMMARY = "summary";
    public static final String FIELD_CHANNELS = "channels";
    
    private String status;
    private String summary;
    private Map<String, Object> channels;
}
