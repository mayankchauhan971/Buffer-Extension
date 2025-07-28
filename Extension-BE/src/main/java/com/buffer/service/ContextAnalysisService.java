package com.buffer.service;

import com.buffer.database.ContentIdeaDatabase;
import com.buffer.dto.OpenAIAnalysisResult;
import com.buffer.entity.*;
import com.buffer.dto.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ContextAnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(ContextAnalysisService.class);
    
    private final OpenAIService openAIService;
    private final ContentIdeaDatabase database;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public ContextAnalysisService(OpenAIService openAIService, ContentIdeaDatabase database) {
        this.openAIService = openAIService;
        this.database = database;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Process screen content and generate structured ideas by channel
     */
    public ContextResponse analyzeScreenContent(ContextRequest request) {
        logger.info("Analyzing screen content for URL: {}", request.getUrl());
        
        // Validate incoming content FIRST - before any processing
        if (request.getFullText() == null || request.getFullText().trim().isEmpty()) {
            logger.warn("EARLY RETURN: Empty or null content received for URL: {} (content: [{}], length: {})", 
                       request.getUrl(), 
                       request.getFullText(), 
                       request.getFullText() != null ? request.getFullText().length() : "NULL");
            return createFailureResponse("", "No content provided for analysis - please ensure the page content is being captured properly by the extension. Check if the page has loaded completely or if there are any content extraction issues.");
        }
        
        logger.info("Content validation passed - processing {} characters", request.getFullText().length());
        
        try {
            // Generate session ID
            String sessionId = openAIService.generateChatId();
            logger.info("Generated session ID: {}", sessionId);
            
            // Create context session from request
            Context session = Context.fromContextRequest(request, sessionId);
            
            // Double-check context was created properly (this shouldn't fail if above check passed)
            if (session.getOriginalContent() == null || session.getOriginalContent().trim().isEmpty()) {
                logger.error("CRITICAL: Content lost during Context creation! Request had {} chars, Context has null/empty content", 
                           request.getFullText().length());
                return createFailureResponse(sessionId, "Content was lost during processing");
            }
            
            logger.debug("Context created successfully with {} chars of content", 
                        session.getOriginalContent().length());
            
            // Get AI analysis - this will only be called if we have content
            logger.info("Making OpenAI call for session: {}", sessionId);
            OpenAIAnalysisResult aiResponse;
            
            // Use channels from request if provided, otherwise use default
            if (request.getChannels() != null && !request.getChannels().isEmpty()) {
                logger.info("Using custom channels from request: {}", request.getChannels());
                aiResponse = openAIService.analyzeContentForIdeas(session, request.getChannels());
            } else {
                logger.info("Using default channels");
                aiResponse = openAIService.analyzeContentForIdeas(session);
            }
            
            if (aiResponse.isSuccess()) {
                logger.info("OpenAI analysis successful for session: {}", sessionId);
                return parseAndStoreAnalysis(aiResponse, session);
            } else {
                logger.warn("OpenAI analysis failed for session: {} - {}", sessionId, aiResponse.getErrorMessage());
                return createFailureResponse(sessionId, "AI analysis failed: " + aiResponse.getErrorMessage());
            }
            
        } catch (Exception e) {
            logger.error("Error analyzing screen content: {}", e.getMessage(), e);
            return createFailureResponse("", "Analysis error: " + e.getMessage());
        }
    }
    
    /**
     * Parse AI response and store the structured data
     */
    private ContextResponse parseAndStoreAnalysis(OpenAIAnalysisResult aiResponse, Context session) {
        try {
            String jsonContent = aiResponse.getContent();
            logger.debug("Parsing AI analysis response: {}", jsonContent);
            
            Map<String, Object> aiData = objectMapper.readValue(jsonContent, Map.class);
            
            String aiStatus = (String) aiData.get("status");
            String summary = (String) aiData.get("summary");
            
            if (!"SUCCESS".equals(aiStatus)) {
                return createFailureResponse(session.getSessionId(), 
                    summary != null ? summary : "AI could not analyze content");
            }
            
            // Parse channels structure
            Map<String, Object> channelsData = (Map<String, Object>) aiData.get("channels");
            if (channelsData != null) {
                parseChannelsStructure(session, channelsData);
            }
            
            // Set the summary in the session before storing
            session.setSummary(summary != null ? summary : "Content analyzed successfully");
            
            // Store complete session in database
            database.storeContextSession(session);
            
            // Build response
            return buildSuccessResponse(session, summary);
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse AI analysis response: {}", e.getMessage());
            return createFailureResponse(session.getSessionId(), "Failed to parse AI response: " + e.getMessage());
        }
    }
    
    /**
     * Parse flat ideas array from AI response
     */
    private void parseIdeasArray(Context session, List<Map<String, Object>> ideasData) {
        // Group ideas by channel
        Map<String, Channel> channelMap = new HashMap<>();
        
        for (Map<String, Object> ideaData : ideasData) {
            String channelName = (String) ideaData.get("channel");
            
            // Get or create channel
            Channel channel = channelMap.get(channelName);
            if (channel == null) {
                channel = Channel.create(session.getSessionId(), channelName);
                channelMap.put(channelName, channel);
            }
            
            // Create idea for this channel
            Idea idea = Idea.create(
                channel.getChannelId(),
                (String) ideaData.get("idea"),
                (String) ideaData.get("rationale"),
                (List<String>) ideaData.get("pros"),
                (List<String>) ideaData.get("cons")
            );
            channel.addIdea(idea);
        }
        
        // Add all channels to session
        for (Channel channel : channelMap.values()) {
            session.addChannel(channel);
            logger.debug("Added channel '{}' with {} ideas", channel.getName().getValue(), channel.getIdeas().size());
        }
    }

    /**
     * Parse channels structure from AI response
     */
    private void parseChannelsStructure(Context session, Map<String, Object> channelsData) {
        for (Map.Entry<String, Object> channelEntry : channelsData.entrySet()) {
            String channelName = channelEntry.getKey();
            List<Map<String, Object>> ideasData = (List<Map<String, Object>>) channelEntry.getValue();
            
            // Create channel
            Channel channel = Channel.create(session.getSessionId(), channelName);
            
            // Parse ideas for this channel
            if (ideasData != null) {
                for (Map<String, Object> ideaData : ideasData) {
                    Idea idea = Idea.create(
                        channel.getChannelId(),
                        (String) ideaData.get("idea"),
                        (String) ideaData.get("rationale"),
                        (List<String>) ideaData.get("pros"),
                        (List<String>) ideaData.get("cons")
                    );
                    channel.addIdea(idea);
                }
            }
            
            session.addChannel(channel);
            logger.debug("Added channel '{}' with {} ideas", channelName, channel.getIdeas().size());
        }
    }
    
    /**
     * Build successful response
     */
    private ContextResponse buildSuccessResponse(Context session, String summary) {
        ContextResponse response = new ContextResponse();
        response.setStatus("SUCCESS");
        response.setChatID(session.getSessionId());
        response.setSummary(summary != null ? summary : "Content analyzed successfully");
        
        // Convert to the format expected by frontend - now supporting multiple ideas per channel
        Map<String, List<IdeaDetailDto>> channelsMap = new LinkedHashMap<>();
        
        for (Channel channel : session.getChannels()) {
            if (!channel.getIdeas().isEmpty()) {
                // Convert all ideas for this channel
                List<IdeaDetailDto> ideaDetailList = new ArrayList<>();
                for (Idea idea : channel.getIdeas()) {
                    IdeaDetailDto ideaDetail = IdeaDetailDto.fromIdea(idea);
                    ideaDetailList.add(ideaDetail);
                }
                channelsMap.put(channel.getName().getValue(), ideaDetailList);
            }
        }
        
        response.setChannels(channelsMap);
        return response;
    }
    
    /**
     * Create failure response
     */
    private ContextResponse createFailureResponse(String sessionId, String errorMessage) {
        ContextResponse response = new ContextResponse();
        response.setStatus("FAILURE");
        response.setChatID(sessionId);
        response.setSummary(errorMessage);
        response.setChannels(new HashMap<>()); // Empty map for channels on failure
        return response;
    }
    
    /**
     * Get a stored context session
     */
    public Context getContextSession(String sessionId) {
        return database.getContextSession(sessionId);
    }
    
    /**
     * Get all sessions (for monitoring)
     */
    public List<Context> getAllSessions() {
        return database.getAllContextSessions();
    }
} 