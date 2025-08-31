package com.buffer.dto;

import lombok.Data;
import java.util.List;

@Data
public class ContentAnalysisRequest {
    private String title;
    private String fullText;
    private String description;
    private String url;
    private List<String> headings;
    private List<String> channels; // Optional: specify which channels to generate ideas for
} 