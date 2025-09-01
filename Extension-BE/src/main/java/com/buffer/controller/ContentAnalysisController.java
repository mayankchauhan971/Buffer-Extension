package com.buffer.controller;

import com.buffer.dto.request.ContentAnalysisRequest;
import com.buffer.dto.response.ContentAnalysisResponse;
import com.buffer.service.ContentAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Content Analysis Controller
 *
 * Handles REST API endpoints for analyzing web content and generating social media content ideas.
 * Processes webpage data (title, content, headings) and returns AI-generated social media strategies
 * for multiple platforms like Instagram, LinkedIn, and X (Twitter).
 */

@Slf4j
@RestController
@Tag(name = "Content Analysis", description = "API for analyzing web content and generating social media ideas")
public class ContentAnalysisController {
    
    private final ContentAnalysisService contentAnalysisService;

    @Autowired
    public ContentAnalysisController(ContentAnalysisService contentAnalysisService) {
        this.contentAnalysisService = contentAnalysisService;
    }

    @Operation(
        summary = "Analyze webpage content",
        description = "Analyzes webpage content and generates social media ideas for multiple platforms using AI"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Content analysis completed successfully",
                    content = @Content(schema = @Schema(implementation = ContentAnalysisResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/api/context")
    public ContentAnalysisResponse analyzeContent(
            @Parameter(description = "Content analysis request containing webpage data", required = true)
            @RequestBody ContentAnalysisRequest request) {

        log.info("Received content analysis request: {}", request);
        return contentAnalysisService.analyzeScreenContent(request);
    }
} 