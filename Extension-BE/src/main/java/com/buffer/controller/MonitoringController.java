package com.buffer.controller;

import com.buffer.database.ContentIdeaDatabase;
import com.buffer.entity.*;
import com.buffer.service.ContextAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class MonitoringController {
    private static final Logger logger = LoggerFactory.getLogger(MonitoringController.class);
    
    private final ContextAnalysisService contextAnalysisService;
    private final ContentIdeaDatabase database;

    @Autowired
    public MonitoringController(ContextAnalysisService contextAnalysisService, 
                               ContentIdeaDatabase database) {
        this.contextAnalysisService = contextAnalysisService;
        this.database = database;
    }

    @GetMapping("/api/monitor/database")
    public Map<String, Object> getDatabaseStats() {
        logger.info("Getting database statistics");
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("timestamp", System.currentTimeMillis());
        response.put("databaseStats", database.getStats());
        
        return response;
    }

    @GetMapping("/api/monitor/session/{sessionId}")
    public Map<String, Object> getSessionData(@PathVariable String sessionId) {
        logger.info("Getting session data for sessionId: {}", sessionId);
        
        Context session = contextAnalysisService.getContextSession(sessionId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        
        if (session != null) {
            response.put("status", "SUCCESS");
            response.put("title", session.getTitle());
            response.put("url", session.getUrl());
            response.put("summary", session.getSummary());
            response.put("createdAt", session.getCreatedAt());
            response.put("channelCount", session.getChannels().size());
            
            int totalIdeas = session.getChannels().stream()
                    .mapToInt(channel -> channel.getIdeas().size())
                    .sum();
            response.put("totalIdeas", totalIdeas);
            
            response.put("channels", session.getChannels());
        } else {
            response.put("status", "NOT_FOUND");
            response.put("message", "No session found for sessionId: " + sessionId);
        }
        
        return response;
    }

    @GetMapping("/api/monitor/sessions")
    public Map<String, Object> getAllSessions() {
        logger.info("Getting all sessions");
        
        List<Context> sessions = contextAnalysisService.getAllSessions();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("sessionCount", sessions.size());
        response.put("sessions", sessions);
        
        return response;
    }
} 