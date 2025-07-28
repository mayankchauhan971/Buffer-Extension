package com.buffer.service;

import com.buffer.dto.OpenAIAnalysisResult;
import com.buffer.entity.*;
import com.buffer.integration.openai.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.*;

@Service
public class OpenAIService {
    private static final Logger logger = LoggerFactory.getLogger(OpenAIService.class);
    
    private final WebClient webClient;

    // Can be extracted from business context stored at Buffer
    private String appContext = "I have a small startup that helps small business collect, manage and analyze their reviews";
    private String targetAudience = "mostly small business owners";
    
    // Default channels configuration
    private List<String> defaultChannels = Arrays.asList("Instagram", "X", "LinkedIn");
    
    public OpenAIService(@Value("${openai.api.key}") String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public void setAppContext(String context) {
        this.appContext = context;
    }

    public void setTargetAudience(String audience) {
        this.targetAudience = audience;
    }

    public String generateChatId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Analyze content for ideas using default channels
     */
    public OpenAIAnalysisResult analyzeContentForIdeas(Context session) {
        return analyzeContentForIdeas(session, defaultChannels);
    }

    /**
     * Analyze content for ideas using specified channels
     */
    public OpenAIAnalysisResult analyzeContentForIdeas(Context session, List<String> channels) {
        logger.info("Starting content analysis for session: {} with channels: {}", session.getSessionId(), channels);

        // Validate content
        if (session.getOriginalContent() == null || session.getOriginalContent().trim().isEmpty()) {
            logger.error("No content to analyze for session: {}", session.getSessionId());
            return OpenAIAnalysisResult.failure("No content provided for analysis");
        }

        // Use provided channels or default
        List<String> channelsToUse = (channels != null && !channels.isEmpty()) ? channels : defaultChannels;
        if (channelsToUse.isEmpty()) {
            logger.warn("No channels provided, using default channels: {}", defaultChannels);
            channelsToUse = defaultChannels;
        }

        logger.debug("Analyzing {} characters of content for {} channels",
                session.getOriginalContent().length(), channelsToUse.size());

        // Prepare messages
        List<OpenAIRequest.Message> messages = new ArrayList<>();
        
        // Add system message
        OpenAIRequest.Message systemMessage = new OpenAIRequest.Message();
        systemMessage.setRole("system");
        systemMessage.setContent(getSystemPrompt(channelsToUse));
        messages.add(systemMessage);
        
        // Add user message with content
        OpenAIRequest.Message userMessage = new OpenAIRequest.Message();
        userMessage.setRole("user");
        userMessage.setContent(session.getOriginalContent());
        messages.add(userMessage);

        logger.debug("Prepared OpenAI request with {} messages, content length: {}, channels: {}",
                messages.size(), session.getOriginalContent().length(), channelsToUse);

        // Call OpenAI with structured output
        return callOpenAIWithStructuredOutput(messages, createResponseFormat(), channelsToUse);
    }

    /**
     * Make API call with structured JSON output
     */
    private OpenAIAnalysisResult callOpenAIWithStructuredOutput(List<OpenAIRequest.Message> messages, ResponseFormat responseFormat, List<String> channels) {
        try {
            OpenAIRequest request = new OpenAIRequest();
            request.setMessages(messages);
            request.setResponseFormat(responseFormat);

            logger.debug("Making OpenAI API call - Model: {}, Messages: {}, Schema: content_ideas_schema",
                        request.getModel(), messages.size());

            OpenAIResponse response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OpenAIResponse.class)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .maxBackoff(Duration.ofSeconds(5))
                            .filter(throwable -> {
                                if (throwable instanceof WebClientResponseException) {
                                    WebClientResponseException wcre = (WebClientResponseException) throwable;
                                    int statusCode = wcre.getStatusCode().value();
                                    // Retry on rate limits, server errors, and some client errors
                                    return statusCode == 429 || // Rate limit
                                           statusCode >= 500 || // Server errors
                                           statusCode == 408;   // Request timeout
                                }
                                return false; // Network errors, connection timeouts
                            }))
                    .block();

            if (response == null) {
                return OpenAIAnalysisResult.failure("Failed to get response from OpenAI after retries");
            }

            String assistantResponse = response.getChoices().get(0).getMessage().getContent();
            
            logger.debug("Received OpenAI response ({} chars), is JSON: {}",
                        assistantResponse.length(), assistantResponse.trim().startsWith("{"));

            // Check if we got JSON - if not, return failure
            if (!assistantResponse.trim().startsWith("{")) {
                logger.error("OpenAI returned plain text instead of structured JSON. Response: {}", assistantResponse);
                return OpenAIAnalysisResult.failure("OpenAI failed to return structured data. Received plain text response instead of JSON.");
            }

            return OpenAIAnalysisResult.success(assistantResponse);
            
        } catch (WebClientResponseException e) {
            String errorBody = e.getResponseBodyAsString();
            logger.error("OpenAI API Error - Status: " + e.getStatusCode() +
                        ", Body: " + errorBody + ", Message: " + e.getMessage());
            return OpenAIAnalysisResult.failure("OpenAI API Error " + e.getStatusCode() + ": " + errorBody);
        } catch (Exception e) {
            logger.error("Unexpected error calling OpenAI: {}", e.getMessage(), e);
            return OpenAIAnalysisResult.failure("Error: " + e.getMessage());
        }
    }

    /**
     * Create response format for structured output
     */
    private ResponseFormat createResponseFormat() {
        try {
            ResponseFormat format = new ResponseFormat();
            format.setType("json_schema");
            
            ResponseFormat.JsonSchema schema = new ResponseFormat.JsonSchema();
            schema.setName("content_ideas_schema");
            schema.setStrict(true);
            
            Map<String, Object> schemaDefinition = createSchemaDefinition();
            schema.setSchema(schemaDefinition);
            
            format.setJsonSchema(schema);
            return format;
        } catch (Exception e) {
            logger.error("Error creating ResponseFormat: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Create JSON schema definition for structured output
     */
    private Map<String, Object> createSchemaDefinition() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        
        Map<String, Object> properties = new HashMap<>();
        
        // Status property
        Map<String, Object> statusProp = new HashMap<>();
        statusProp.put("type", "string");
        statusProp.put("enum", Arrays.asList("SUCCESS", "FAILURE"));
        properties.put("status", statusProp);
        
        // Summary property
        Map<String, Object> summaryProp = new HashMap<>();
        summaryProp.put("type", "string");
        properties.put("summary", summaryProp);
        

        
        // Channels property - simplified structure for the channels object
        Map<String, Object> channelsProp = new HashMap<>();
        channelsProp.put("type", "object");
        channelsProp.put("additionalProperties", false);
        
        // Define properties for specific channels (instagram, X, linkedin)
        Map<String, Object> channelProperties = new HashMap<>();
        
        // Create idea array definition
        Map<String, Object> ideaArray = new HashMap<>();
        ideaArray.put("type", "array");
        ideaArray.put("minItems", 1);
        ideaArray.put("maxItems", 5);
        
        Map<String, Object> ideaItem = new HashMap<>();
        ideaItem.put("type", "object");
        ideaItem.put("additionalProperties", false);
        
        Map<String, Object> ideaItemProps = new HashMap<>();
        ideaItemProps.put("idea", Map.of("type", "string"));
        ideaItemProps.put("rationale", Map.of("type", "string"));
        ideaItemProps.put("pros", Map.of("type", "array", "items", Map.of("type", "string")));
        ideaItemProps.put("cons", Map.of("type", "array", "items", Map.of("type", "string")));
        
        ideaItem.put("properties", ideaItemProps);
        ideaItem.put("required", Arrays.asList("idea", "rationale", "pros", "cons"));
        
        ideaArray.put("items", ideaItem);
        
        // Add each channel as a property
        channelProperties.put("instagram", ideaArray);
        channelProperties.put("X", ideaArray);
        channelProperties.put("linkedin", ideaArray);
        
        channelsProp.put("properties", channelProperties);
        channelsProp.put("required", Arrays.asList("instagram", "X", "linkedin"));
        
        properties.put("channels", channelsProp);
        
        schema.put("properties", properties);
        schema.put("required", Arrays.asList("status", "summary", "channels"));
        
        return schema;
    }

    /**
     * Generate system prompt for content analysis
     */
    private String getSystemPrompt(List<String> channels) {
        return "You are an expert social media strategist and content ideation assistant.\n" +
           "You need to first generate a concise summary of the content answering what is the main idea of the content. Keep it super short and concise. It should be 2-3 sentences." +
           "Then your task is to generate 3 unique, actionable content ideas per channel, 3 for each of the following social media channels: " + String.join(", ", channels) + ". \n\n" +
           "Each idea should:\n" +
           "- Be highly specific and detailed about the idea so that generating content from idea is easy.\n" +
           "- Be tailored to the selected platform’s format, audience behavior, and content trends keeping in mind what works and what not\n" +
           "- Reflect the business's voice, tone, and business context.\n\n" +
           "For each idea, provide:\n" +
           "1. A clear and creative content idea\n" +
           "2. Why it would perform well on the specific platform (platform rationale), why would users love it or find it useful\n" +
           "3. 2-3 benefits of posting it (pros)\n" +
           "4. 1-2 potential limitations (cons)\n\n" +
           "Make sure ideas are deeply personalized and practical, avoiding vague or generic suggestions.\n" +
           "Keep the tone helpful and professional.\n\n" +
           "Business context: " + appContext + "\n" +
           "Target audience: " + targetAudience + "\n\n" +
           "Return your response as valid JSON with the following structure:\n" +
           "{\n" +
           "  \"status\": \"SUCCESS\",\n" +
           "  \"summary\": \"brief summary of the content\",\n" +
           "  \"channels\": {\n" +
           "    \"instagram\": [array of idea objects],\n" +
           "    \"X\": [array of idea objects],\n" +
           "    \"linkedin\": [array of idea objects]\n" +
           "  }\n" +
           "}\n" +
           "Each idea object should contain: idea, rationale, pros, cons.\n\n";

    }

} 