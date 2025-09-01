package com.buffer.service;

import com.buffer.dto.common.IdeaDetailDto;
import com.buffer.dto.request.ContentAnalysisRequest;
import com.buffer.dto.response.ContentAnalysisResponse;
import com.buffer.dto.response.AIAnalysisResponseDto;
import com.buffer.repository.AnalysisSessionRepository;
import com.buffer.dto.common.OpenAIServiceResult;
import com.buffer.entity.*;
import com.buffer.enums.ContentAnalysisStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Content Analysis Service
 *
 * Core business logic service that orchestrates the content analysis workflow.
 * Processes web content through AI analysis, manages analysis sessions, and stores
 * generated social media ideas. Handles the complete pipeline from content validation
 * to structured response generation for multiple social media platforms.
 */

@Slf4j
@Service
public class ContentAnalysisService {
    
    private final OpenAIService openAIService;
    private final AnalysisSessionRepository repository;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public ContentAnalysisService(OpenAIService openAIService, AnalysisSessionRepository repository) {
        this.openAIService = openAIService;
        this.repository = repository;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Process screen content and generate structured ideas by channel
     */
    public ContentAnalysisResponse analyzeScreenContent(ContentAnalysisRequest request) {
        log.info("Analyzing screen content for URL: {}", request.getUrl());
        
        if (request.getFullText() == null || request.getFullText().trim().isEmpty()) {
            log.warn("EARLY RETURN: Empty or null content received for URL: {} (content: [{}], length: {})", 
                       request.getUrl(), 
                       request.getFullText(), 
                       request.getFullText() != null ? request.getFullText().length() : "NULL");
            return createFailureResponse("", "No content provided for analysis - please ensure the page content is being captured properly by the extension. Check if the page has loaded completely or if there are any content extraction issues.");
        }

        try {
            String sessionId = openAIService.generateChatId();
            log.info("Generated session ID: {}", sessionId);
            
            AnalysisSession session = AnalysisSession.fromContentAnalysisRequest(request, sessionId);
            
            if (session.getOriginalContent() == null || session.getOriginalContent().trim().isEmpty()) {
                log.error("CRITICAL: Content lost during Context creation! Request had {} chars, Context has null/empty content", 
                           request.getFullText().length());
                return createFailureResponse(sessionId, "Content was lost during processing");
            }
            
            log.debug("Context created successfully with {} chars of content", 
                        session.getOriginalContent().length());
            
            log.info("Making OpenAI call for session: {}", sessionId);
            OpenAIServiceResult aiResponse;
            
            // Check content length and potentially truncate for better success rate
            if (session.getOriginalContent().length() > com.buffer.config.AIConstants.MAX_CONTENT_LENGTH) {
                log.warn("Content length ({} chars) exceeds maximum. Truncating to {} chars for analysis.", 
                        session.getOriginalContent().length(), com.buffer.config.AIConstants.TRUNCATED_CONTENT_LENGTH);
                session.setOriginalContent(session.getOriginalContent().substring(0, com.buffer.config.AIConstants.TRUNCATED_CONTENT_LENGTH));
            }
            
            if (request.getChannels() != null && !request.getChannels().isEmpty()) {
                log.info("Using custom channels from request: {}", request.getChannels());
                aiResponse = openAIService.analyzeContentForIdeas(session, request.getChannels());
            } else {
                log.info("Using default channels");
                aiResponse = openAIService.analyzeContentForIdeas(session);
            }
            
            if (aiResponse.isSuccess()) {
                log.info("OpenAI analysis successful for session: {}", sessionId);
                ContentAnalysisResponse response = parseAndStoreAnalysis(aiResponse, session);
                
                // If parsing failed due to truncated response, try once more with shorter content
                if (response.getStatus() == ContentAnalysisStatus.FAILURE && 
                    response.getSummary().contains("incomplete response") &&
                    session.getOriginalContent().length() > com.buffer.config.AIConstants.TRUNCATED_CONTENT_LENGTH) {
                    
                    log.warn("Retrying analysis with shorter content for session: {}", sessionId);
                    session.setOriginalContent(session.getOriginalContent().substring(0, com.buffer.config.AIConstants.TRUNCATED_CONTENT_LENGTH));
                    
                    OpenAIServiceResult retryResponse;
                    if (request.getChannels() != null && !request.getChannels().isEmpty()) {
                        retryResponse = openAIService.analyzeContentForIdeas(session, request.getChannels());
                    } else {
                        retryResponse = openAIService.analyzeContentForIdeas(session);
                    }
                    
                    if (retryResponse.isSuccess()) {
                        log.info("Retry analysis successful for session: {}", sessionId);
                        return parseAndStoreAnalysis(retryResponse, session);
                    }
                }
                
                return response;
            } else {
                log.warn("OpenAI analysis failed for session: {} - {}", sessionId, aiResponse.getErrorMessage());
                return createFailureResponse(sessionId, "AI analysis failed: " + aiResponse.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("Error analyzing screen content: {}", e.getMessage(), e);
            return createFailureResponse("", "Analysis error: " + e.getMessage());
        }
    }
    
    /**
     * Parse AI response and store the structured data
     */
    private ContentAnalysisResponse parseAndStoreAnalysis(OpenAIServiceResult aiResponse, AnalysisSession session) {
        try {
            String jsonContent = aiResponse.getContent();
            log.debug("Parsing AI analysis response length: {} chars", jsonContent != null ? jsonContent.length() : 0);
            
            // Validate JSON completeness before parsing
            if (!isValidJsonStructure(jsonContent)) {
                log.error("Invalid or truncated JSON response detected. Response length: {}, Content preview: {}", 
                         jsonContent != null ? jsonContent.length() : 0,
                         jsonContent != null && jsonContent.length() > 200 ? 
                             jsonContent.substring(0, 200) + "..." : jsonContent);
                return createFailureResponse(session.getSessionId(), 
                    "Received incomplete response from AI service. Please try again.");
            }
            
            Map<String, Object> aiData = objectMapper.readValue(jsonContent, Map.class);
            
            String aiStatus = (String) aiData.get(AIAnalysisResponseDto.FIELD_STATUS);
            String summary = (String) aiData.get(AIAnalysisResponseDto.FIELD_SUMMARY);
            
            if (!ContentAnalysisStatus.SUCCESS.name().equals(aiStatus)) {
                return createFailureResponse(session.getSessionId(), 
                    summary != null ? summary : "AI could not analyze content");
            }
            
            Map<String, Object> channelsData = (Map<String, Object>) aiData.get(AIAnalysisResponseDto.FIELD_CHANNELS);
            if (channelsData != null) {
                parseChannelsStructure(session, channelsData);
            }
            
            session.setSummary(summary != null ? summary : "Content analyzed successfully");
            
            repository.storeSession(session);
            
            return buildSuccessResponse(session, summary);
            
        } catch (JsonProcessingException e) {
            log.error("Failed to parse AI analysis response: {}", e.getMessage());
            log.error("Response content length: {}", aiResponse.getContent() != null ? aiResponse.getContent().length() : 0);
            if (aiResponse.getContent() != null && aiResponse.getContent().length() > 500) {
                log.error("Response start: {}", aiResponse.getContent().substring(0, 500));
                log.error("Response end: {}", aiResponse.getContent().substring(Math.max(0, aiResponse.getContent().length() - 500)));
            }
            return createFailureResponse(session.getSessionId(), "Failed to parse AI response: " + e.getMessage());
        }
    }
    
    /**
     * Validate JSON structure to detect truncated or malformed responses
     */
    private boolean isValidJsonStructure(String jsonContent) {
        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            log.warn("JSON content is null or empty");
            return false;
        }
        
        String trimmed = jsonContent.trim();
        
        // Basic structure checks
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            log.warn("JSON doesn't start with {{ or end with }}: starts={}, ends={}", 
                    trimmed.length() > 0 ? trimmed.charAt(0) : "empty",
                    trimmed.length() > 0 ? trimmed.charAt(trimmed.length() - 1) : "empty");
            return false;
        }
        
        // Check for balanced braces and brackets
        int braceCount = 0;
        int bracketCount = 0;
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            
            if (escaped) {
                escaped = false;
                continue;
            }
            
            if (c == '\\') {
                escaped = true;
                continue;
            }
            
            if (c == '"' && !escaped) {
                inString = !inString;
                continue;
            }
            
            if (!inString) {
                switch (c) {
                    case '{':
                        braceCount++;
                        break;
                    case '}':
                        braceCount--;
                        break;
                    case '[':
                        bracketCount++;
                        break;
                    case ']':
                        bracketCount--;
                        break;
                }
            }
        }
        
        if (braceCount != 0 || bracketCount != 0) {
            log.warn("Unbalanced JSON structure: braces={}, brackets={}", braceCount, bracketCount);
            return false;
        }
        
        // Additional check: try to parse with ObjectMapper to catch subtle JSON errors
        try {
            objectMapper.readTree(trimmed);
            return true;
        } catch (JsonProcessingException e) {
            log.warn("JSON structure validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Parse flat ideas array from AI response
     */
    private void parseIdeasArray(AnalysisSession session, List<Map<String, Object>> ideasData) {
        // Group ideas by channel
        Map<String, SocialMediaChannel> channelMap = new HashMap<>();
        
        for (Map<String, Object> ideaData : ideasData) {
            String channelName = (String) ideaData.get(IdeaDetailDto.FIELD_CHANNEL);
            
            // Get or create channel
            SocialMediaChannel socialMediaChannel = channelMap.get(channelName);
            if (socialMediaChannel == null) {
                socialMediaChannel = SocialMediaChannel.create(session, channelName);
                channelMap.put(channelName, socialMediaChannel);
            }
            
            // Create idea for this channel
            ContentIdea contentIdea = ContentIdea.create(
                socialMediaChannel,
                (String) ideaData.get(IdeaDetailDto.FIELD_IDEA),
                (String) ideaData.get(IdeaDetailDto.FIELD_RATIONALE),
                (List<String>) ideaData.get(IdeaDetailDto.FIELD_PROS),
                (List<String>) ideaData.get(IdeaDetailDto.FIELD_CONS)
            );
            socialMediaChannel.addIdea(contentIdea);
        }
        
        // Add all channels to session
        for (SocialMediaChannel socialMediaChannel : channelMap.values()) {
            session.addChannel(socialMediaChannel);
            log.debug("Added channel '{}' with {} ideas", socialMediaChannel.getName().name(), socialMediaChannel.getContentIdeas().size());
        }
    }

    /**
     * Parse channels structure from AI response
     */
    private void parseChannelsStructure(AnalysisSession session, Map<String, Object> channelsData) {
        for (Map.Entry<String, Object> channelEntry : channelsData.entrySet()) {
            String channelName = channelEntry.getKey();
            List<Map<String, Object>> ideasData = (List<Map<String, Object>>) channelEntry.getValue();
            
            // Create channel
            SocialMediaChannel socialMediaChannel = SocialMediaChannel.create(session, channelName);
            
            // Parse ideas for this channel
            if (ideasData != null) {
                for (Map<String, Object> ideaData : ideasData) {
                    ContentIdea contentIdea = ContentIdea.create(
                        socialMediaChannel,
                        (String) ideaData.get(IdeaDetailDto.FIELD_IDEA),
                        (String) ideaData.get(IdeaDetailDto.FIELD_RATIONALE),
                        (List<String>) ideaData.get(IdeaDetailDto.FIELD_PROS),
                        (List<String>) ideaData.get(IdeaDetailDto.FIELD_CONS)
                    );
                    socialMediaChannel.addIdea(contentIdea);
                }
            }
            
            session.addChannel(socialMediaChannel);
            log.debug("Added channel '{}' with {} ideas", channelName, socialMediaChannel.getContentIdeas().size());
        }
    }
    
    /**
     * Build successful response
     */
    private ContentAnalysisResponse buildSuccessResponse(AnalysisSession session, String summary) {
        ContentAnalysisResponse response = new ContentAnalysisResponse();
        response.setStatus(ContentAnalysisStatus.SUCCESS);
        response.setChatID(session.getSessionId());
        response.setSummary(summary != null ? summary : "Content analyzed successfully");
        
        // Convert to the format expected by frontend - now supporting multiple ideas per channel
        Map<String, List<IdeaDetailDto>> channelsMap = new LinkedHashMap<>();
        
        for (SocialMediaChannel socialMediaChannel : session.getSocialMediaChannels()) {
            if (!socialMediaChannel.getContentIdeas().isEmpty()) {
                // Convert all ideas for this channel
                List<IdeaDetailDto> ideaDetailList = new ArrayList<>();
                for (ContentIdea contentIdea : socialMediaChannel.getContentIdeas()) {
                    IdeaDetailDto ideaDetail = IdeaDetailDto.fromIdea(contentIdea);
                    ideaDetailList.add(ideaDetail);
                }
                channelsMap.put(socialMediaChannel.getName().name(), ideaDetailList);
            }
        }
        
        response.setChannels(channelsMap);
        return response;
    }
    
    /**
     * Create failure response
     */
    private ContentAnalysisResponse createFailureResponse(String sessionId, String errorMessage) {
        ContentAnalysisResponse response = new ContentAnalysisResponse();
        response.setStatus(ContentAnalysisStatus.FAILURE);
        response.setChatID(sessionId);
        response.setSummary(errorMessage);
        response.setChannels(new HashMap<>()); // Empty map for channels on failure
        return response;
    }
    
    /**
     * Get a stored context session
     */
    public AnalysisSession getContextSession(String sessionId) {
        return repository.getSession(sessionId);
    }
    
    /**
     * Get all sessions (for monitoring)
     */
    public List<AnalysisSession> getAllSessions() {
        return repository.getAllSessions();
    }
} 