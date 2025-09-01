package com.buffer.dto.request;

import lombok.Data;
import java.util.List;

/**
 * Content Analysis Request DTO
 *
 * Data transfer object representing a request for content analysis from browser extensions
 * or client applications. Contains webpage metadata (title, description, URL), extracted
 * content (full text, headings), and optional channel preferences for targeted social
 * media idea generation.
 */
@Data
public class ContentAnalysisRequest {
    private String title;
    private String fullText;
    private String description;
    private String url;
    private List<String> headings;
    private List<String> channels;
} 