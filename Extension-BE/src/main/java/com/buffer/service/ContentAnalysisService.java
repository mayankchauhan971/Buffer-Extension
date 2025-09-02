package com.buffer.service;

import com.buffer.domain.dto.common.IdeaDetailDto;
import com.buffer.domain.dto.request.ContentAnalysisRequest;
import com.buffer.domain.dto.response.ContentAnalysisResponse;

import com.buffer.domain.dto.response.OpenAIAnalysisDto;
import com.buffer.repository.AnalysisSessionRepository;
import com.buffer.domain.dto.common.OpenAIServiceResult;
import com.buffer.domain.entity.*;
import com.buffer.domain.enums.ContentAnalysisStatus;
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
    
    // Message constants
    private static final class Messages {
        static final String NO_CONTENT = "No content provided for analysis - please ensure the page content is being " +
                "captured properly by the extension. Check if the page has loaded completely or if there are any " +
                "content extraction issues.";
        static final String INCOMPLETE_RESPONSE = "Received incomplete response from AI service. Please try again.";
        static final String AI_COULD_NOT_ANALYZE = "AI could not analyze content";
        static final String CONTENT_ANALYZED_SUCCESSFULLY = "Content analyzed successfully";
        
        static final String ANALYSIS_ERROR_PREFIX = "Analysis error: ";
        static final String PARSE_AI_RESPONSE_PREFIX = "Failed to parse AI response: ";
        
        static final String INCOMPLETE_RESPONSE_KEYWORD = "incomplete response";
    }
    
    // Log message templates
    private static final class LogMessages {
        static final String EARLY_RETURN = "EARLY RETURN: Empty or null content received for URL: {} (content: [{}], length: {})";
        static final String OPENAI_FAILED = "OpenAI analysis failed for session: {} - {}";
        static final String ERROR_ANALYZING = "Error analyzing screen content: {}";
        static final String INVALID_JSON = "Invalid or truncated JSON response detected. {}";
        static final String PARSE_FAILED = "Failed to parse AI analysis response: {} | Content length: {} ";
        static final String JSON_NULL_EMPTY = "JSON content is null or empty";
        static final String JSON_STRUCTURE = "JSON doesn't start with {{ or end with }}: {}";
        static final String UNBALANCED_JSON = "Unbalanced JSON structure: braces={}, brackets={}";
        static final String JSON_VALIDATION_FAILED = "JSON structure validation failed: {}";
    }

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

        if (request.getFullText() == null || request.getFullText().trim().isEmpty()) {
            log.warn(LogMessages.EARLY_RETURN, 
                       request.getUrl(), 
                       request.getFullText(),
                       request.getFullText().length());
            return createFailureResponse("", Messages.NO_CONTENT);
        }

        try {
            String sessionId = openAIService.generateChatId();
            AnalysisSession session = AnalysisSession.fromContentAnalysisRequest(request, sessionId);

            OpenAIServiceResult aiResponse;
            
            // Tradeoff between availability and accuracy - truncating long content for higher success rate with OpenAI
            if (session.getOriginalContent().length() > com.buffer.web.config.AIConstants.MAX_CONTENT_LENGTH) {
                session.setOriginalContent(session.getOriginalContent().substring(0, com.buffer.web.config.AIConstants.TRUNCATED_CONTENT_LENGTH));
            }

            if (request.getChannels() != null && !request.getChannels().isEmpty()) {
                aiResponse = openAIService.analyzeContentForIdeas(session, request.getChannels());
            } else {
                aiResponse = openAIService.analyzeContentForIdeas(session);
            }
            
            if (aiResponse.isSuccess()) {
                ContentAnalysisResponse response = parseAndStoreAnalysis(aiResponse, session);
                
                // If parsing failed due to truncated response, try once more with shorter content
                if (response.getStatus() == ContentAnalysisStatus.FAILURE && 
                    response.getSummary().contains(Messages.INCOMPLETE_RESPONSE_KEYWORD) &&
                    session.getOriginalContent().length() > com.buffer.web.config.AIConstants.TRUNCATED_CONTENT_LENGTH) {
                    session.setOriginalContent(session.getOriginalContent().substring(0, com.buffer.web.config.AIConstants.TRUNCATED_CONTENT_LENGTH));
                    
                    OpenAIServiceResult retryResponse;
                    if (request.getChannels() != null && !request.getChannels().isEmpty()) {
                        retryResponse = openAIService.analyzeContentForIdeas(session, request.getChannels());
                    } else {
                        retryResponse = openAIService.analyzeContentForIdeas(session);
                    }
                    
                    if (retryResponse.isSuccess()) {
                        return parseAndStoreAnalysis(retryResponse, session);
                    }
                }
                
                return response;
            } else {
                log.warn(LogMessages.OPENAI_FAILED, sessionId, aiResponse.getErrorMessage());
                return createFailureResponse(sessionId, Messages.ANALYSIS_ERROR_PREFIX + aiResponse.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error(LogMessages.ERROR_ANALYZING, e.getMessage(), e);
            return createFailureResponse("", Messages.ANALYSIS_ERROR_PREFIX + e.getMessage());
        }
    }
    
    /**
     * Parse AI response and store the structured data
     */
    private ContentAnalysisResponse parseAndStoreAnalysis(OpenAIServiceResult aiResponse, AnalysisSession session) {
        try {
            String jsonContent = aiResponse.getContent();

            // Additional safety net to check valid json despite using structured output
            if (!isValidJsonStructure(jsonContent)) {
                log.error(LogMessages.INVALID_JSON, jsonContent);
                return createFailureResponse(session.getSessionId(), 
                    Messages.INCOMPLETE_RESPONSE);
            }
            
            OpenAIAnalysisDto aiData = objectMapper.readValue(jsonContent, OpenAIAnalysisDto.class);
            
            String aiStatus = aiData.getStatus();
            String summary = aiData.getSummary();
            
            if (!ContentAnalysisStatus.SUCCESS.name().equals(aiStatus)) {
                return createFailureResponse(session.getSessionId(), 
                    summary != null ? summary : Messages.AI_COULD_NOT_ANALYZE);
            }
            
            Map<String, List<IdeaDetailDto>> channelsData = aiData.getChannels();
            if (channelsData != null) {
                parseChannelsStructure(session, channelsData);
            }
            
            session.setSummary(summary != null ? summary : Messages.CONTENT_ANALYZED_SUCCESSFULLY);
            
            repository.storeSession(session);
            
            return buildSuccessResponse(session, summary);
            
        } catch (JsonProcessingException e) {
            String content = aiResponse.getContent();
            log.error(LogMessages.PARSE_FAILED, e.getMessage(), content.length());
            return createFailureResponse(session.getSessionId(), Messages.PARSE_AI_RESPONSE_PREFIX + e.getMessage());
        }
    }
    
    /**
     * Validate JSON structure to detect truncated or malformed responses
     */
    private boolean isValidJsonStructure(String jsonContent) {
        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            log.warn(LogMessages.JSON_NULL_EMPTY);
            return false;
        }
        
        String trimmed = jsonContent.trim();
        
        // Basic structure checks
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            log.warn(LogMessages.JSON_STRUCTURE, trimmed);
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
            log.warn(LogMessages.UNBALANCED_JSON, braceCount, bracketCount);
            return false;
        }
        
        // Additional check: try to parse with ObjectMapper to catch subtle JSON errors
        try {
            objectMapper.readTree(trimmed);
            return true;
        } catch (JsonProcessingException e) {
            log.warn(LogMessages.JSON_VALIDATION_FAILED, e.getMessage());
            return false;
        }
    }

    /**
     * Parse channels structure from AI response
     */
    private void parseChannelsStructure(AnalysisSession session, Map<String, List<IdeaDetailDto>> channelsData) {
        for (Map.Entry<String, List<IdeaDetailDto>> channelEntry : channelsData.entrySet()) {
            String channelName = channelEntry.getKey();
            List<IdeaDetailDto> ideasData = channelEntry.getValue();
            
            // Create channel
            SocialMediaChannel socialMediaChannel = SocialMediaChannel.create(session, channelName);
            
            // Parse ideas for this channel
            if (ideasData != null) {
                for (IdeaDetailDto ideaData : ideasData) {
                    ContentIdea contentIdea = ContentIdea.create(
                        socialMediaChannel,
                        ideaData.getIdea(),
                        ideaData.getRationale(),
                        ideaData.getPros(),
                        ideaData.getCons()
                    );
                    socialMediaChannel.addIdea(contentIdea);
                }
            }
            
            session.addChannel(socialMediaChannel);
        }
    }
    
    /**
     * Build successful response
     */
    private ContentAnalysisResponse buildSuccessResponse(AnalysisSession session, String summary) {
        ContentAnalysisResponse response = new ContentAnalysisResponse();
        response.setStatus(ContentAnalysisStatus.SUCCESS);
        response.setChatID(session.getSessionId());
        response.setSummary(summary != null ? summary : Messages.CONTENT_ANALYZED_SUCCESSFULLY);
        
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
        response.setChannels(new HashMap<>());
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