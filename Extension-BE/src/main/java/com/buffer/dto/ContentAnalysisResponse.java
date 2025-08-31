package com.buffer.dto;

import lombok.Data;
import java.util.Map;
import java.util.List;

@Data
public class ContentAnalysisResponse {
    private String summary;
    private String chatID;
    private String status; // SUCCESS, FAILURE, TIMEOUT
    private Map<String, List<IdeaDetailDto>> channels; 
} 