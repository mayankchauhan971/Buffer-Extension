package com.buffer.domain.dto.response;

import com.buffer.domain.dto.common.IdeaDetailDto;
import lombok.Data;
import java.util.Map;
import java.util.List;
import com.buffer.domain.enums.ContentAnalysisStatus;

/**
 * Content Analysis Response DTO
 *
 * DTO representing the response from content analysis operations.
 * Contains analysis status, session ID for tracking, content summary, and organized
 * social media ideas grouped by platform. Used to return structured AI-generated
 * content strategies to client applications.
 */
@Data
public class ContentAnalysisResponse {
    private String summary;
    private String chatID;
    private ContentAnalysisStatus status;
    private Map<String, List<IdeaDetailDto>> channels;
} 