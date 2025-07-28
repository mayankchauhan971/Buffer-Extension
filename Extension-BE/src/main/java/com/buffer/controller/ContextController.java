package com.buffer.controller;

import com.buffer.dto.ContextRequest;
import com.buffer.dto.ContextResponse;
import com.buffer.service.ContextAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ContextController {
    private static final Logger logger = LoggerFactory.getLogger(ContextController.class);
    
    private final ContextAnalysisService contextAnalysisService;

    @Autowired
    public ContextController(ContextAnalysisService contextAnalysisService) {
        this.contextAnalysisService = contextAnalysisService;
    }

    @PostMapping("/api/context")
    public ContextResponse analyzeContent(@RequestBody ContextRequest request) {
        // Log detailed request information
        logger.info("=== INCOMING REQUEST DEBUG ===");
        logger.info("URL: [{}]", request.getUrl());
        logger.info("Title: [{}]", request.getTitle());
        logger.info("Description: [{}]", request.getDescription());
        logger.info("Full Text: [{}]", request.getFullText());
        logger.info("Full Text Length: {}", request.getFullText() != null ? request.getFullText().length() : "NULL");
        logger.info("Full Text isEmpty: {}", request.getFullText() != null ? request.getFullText().trim().isEmpty() : "NULL");
        logger.info("Headings: {}", request.getHeadings());
        logger.info("Headings count: {}", request.getHeadings() != null ? request.getHeadings().size() : "NULL");
        logger.info("===============================");
        
        return contextAnalysisService.analyzeScreenContent(request);
    }
} 