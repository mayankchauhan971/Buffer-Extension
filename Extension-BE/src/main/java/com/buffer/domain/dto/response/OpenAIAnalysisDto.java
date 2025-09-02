package com.buffer.domain.dto.response;

import com.buffer.domain.dto.common.IdeaDetailDto;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Map;
import java.util.List;

/**
 * OpenAI Analysis DTO
 *
 * Strongly typed DTO representing the complete structure of AI response from OpenAI service.
 * Replaces the generic Map<String, Object> usage with proper type safety.
 * Contains all fields that can be returned by the AI analysis service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAIAnalysisDto {
    
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_SUMMARY = "summary";
    public static final String FIELD_CHANNELS = "channels";
    
    private String status;
    private String summary;
    private Map<String, List<IdeaDetailDto>> channels;
}
