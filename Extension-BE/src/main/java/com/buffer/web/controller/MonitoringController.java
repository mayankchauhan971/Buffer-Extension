package com.buffer.web.controller;

import com.buffer.domain.dto.response.DatabaseHealthResponse;
import com.buffer.domain.dto.response.SessionDataResponse;
import com.buffer.domain.dto.response.SessionsListResponse;
import com.buffer.domain.entity.*;
import com.buffer.service.ContentAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Monitoring Controller
 *
 * Provides monitoring and diagnostic endpoints for tracking content analysis sessions,
 * system health, and database statistics. Used for debugging and operational insights
 * into the content analysis workflows and stored session data.
 */

@Slf4j
@RestController
@Tag(name = "Monitoring", description = "API for monitoring sessions and system health")
public class MonitoringController {
    
    private final ContentAnalysisService contentAnalysisService;

    @Autowired
    public MonitoringController(ContentAnalysisService contentAnalysisService) {
        this.contentAnalysisService = contentAnalysisService;
    }

    @Operation(
        summary = "Get session data",
        description = "Retrieves detailed information for a specific analysis session"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Session data retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Session not found")
    })
    @GetMapping("/api/monitor/session/{sessionId}")
    public SessionDataResponse getSessionData(
            @Parameter(description = "Unique identifier of the session", required = true)
            @PathVariable String sessionId) {
        log.info("Getting session data for sessionId: {}", sessionId);
        
        AnalysisSession session = contentAnalysisService.getContextSession(sessionId);
        
        if (session != null) {
            int totalIdeas = session.getSocialMediaChannels().stream()
                    .mapToInt(channel -> channel.getContentIdeas().size())
                    .sum();
                    
            return SessionDataResponse.builder()
                    .sessionId(sessionId)
                    .status("SUCCESS")
                    .title(session.getTitle())
                    .url(session.getUrl())
                    .summary(session.getSummary())
                    .createdAt(session.getCreatedAt())
                    .channelCount(session.getSocialMediaChannels().size())
                    .totalIdeas(totalIdeas)
                    .channels(session.getSocialMediaChannels())
                    .build();
        } else {
            return SessionDataResponse.builder()
                    .sessionId(sessionId)
                    .status("NOT_FOUND")
                    .message("No session found for sessionId: " + sessionId)
                    .build();
        }
    }

    @Operation(
        summary = "Get all sessions",
        description = "Lists all content analysis sessions with their basic information"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sessions retrieved successfully")
    })
    @GetMapping("/api/monitor/sessions")
    public SessionsListResponse getAllSessions() {
        log.info("Getting all sessions");
        
        List<AnalysisSession> sessions = contentAnalysisService.getAllSessions();
        
        return SessionsListResponse.builder()
                .status("SUCCESS")
                .sessionCount(sessions.size())
                .sessions(sessions)
                .build();
    }

    @Operation(
        summary = "Get database health",
        description = "Returns database statistics and health information"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Database health information retrieved successfully")
    })
    @GetMapping("/api/monitor/database")
    public DatabaseHealthResponse getDatabaseHealth() {
        log.info("Getting database health information");
        
        List<AnalysisSession> sessions = contentAnalysisService.getAllSessions();
        
        int totalIdeas = sessions.stream()
                .mapToInt(session -> session.getSocialMediaChannels().stream()
                        .mapToInt(channel -> channel.getContentIdeas().size())
                        .sum())
                .sum();
                
        int totalChannels = sessions.stream()
                .mapToInt(session -> session.getSocialMediaChannels().size())
                .sum();
        
        return DatabaseHealthResponse.builder()
                .status("HEALTHY")
                .sessionCount(sessions.size())
                .totalIdeas(totalIdeas)
                .totalChannels(totalChannels)
                .build();
    }
} 