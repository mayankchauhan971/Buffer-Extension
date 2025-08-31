package com.buffer.service;

import com.buffer.dto.OpenAIServiceResult;
import com.buffer.entity.*;
import com.buffer.config.AIConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.core.ParameterizedTypeReference;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.*;

@Service
public class OpenAIService {
    private static final Logger logger = LoggerFactory.getLogger(OpenAIService.class);
    
    private final WebClient webClient;

    // Can be extracted from business context stored at Buffer
    private String appContext = AIConstants.BUSINESS_CONTEXT;
    private String targetAudience = AIConstants.TARGET_AUDIENCE;
    
    // Default channels configuration
    private List<String> defaultChannels = AIConstants.DEFAULT_CHANNELS;
    
    public OpenAIService(@Value("${openai.api.key}") String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl(AIConstants.OPENAI_BASE_URL)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public String generateChatId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Analyze content for ideas using default channels
     */
    public OpenAIServiceResult analyzeContentForIdeas(AnalysisSession session) {
        return analyzeContentForIdeas(session, defaultChannels);
    }

    /**
     * Analyze content for ideas using specified channels
     */
    public OpenAIServiceResult analyzeContentForIdeas(AnalysisSession session, List<String> channels) {
        logger.info("Starting content analysis for session: {} with channels: {}", session.getSessionId(), channels);

        // Validate content
        if (session.getOriginalContent() == null || session.getOriginalContent().trim().isEmpty()) {
            logger.error("No content to analyze for session: {}", session.getSessionId());
            return OpenAIServiceResult.failure("No content provided for analysis");
        }

        // Use provided channels or default
        List<String> channelsToUse = (channels != null && !channels.isEmpty()) ? channels : defaultChannels;
        if (channelsToUse.isEmpty()) {
            logger.warn("No channels provided, using default channels: {}", defaultChannels);
            channelsToUse = defaultChannels;
        }
        // Normalize to canonical schema keys and de-duplicate while preserving order
        List<String> normalizedChannels = normalizeChannels(channelsToUse);

        logger.debug("Analyzing {} characters of content for {} channels",
                session.getOriginalContent().length(), normalizedChannels.size());

        // Prepare instructions and input for Responses API
        String instructions = getSystemPrompt(normalizedChannels);
        String input = session.getOriginalContent();

        logger.debug("Prepared OpenAI responses request, content length: {}, channels: {}",
                input.length(), normalizedChannels);

        // Call OpenAI with structured output via Responses API
        return callOpenAIWithStructuredOutput(instructions, input, createTextFormat(normalizedChannels), normalizedChannels);
    }

    /**
     * Make API call with structured JSON output
     */
    private OpenAIServiceResult callOpenAIWithStructuredOutput(String instructions, String input, Map<String, Object> textFormat, List<String> channels) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("model", AIConstants.OPENAI_MODEL);
            request.put("instructions", instructions);
            request.put("input", input);
            Map<String, Object> textOptions = new HashMap<>();
            textOptions.put("format", textFormat);
            request.put("text", textOptions);
            request.put("temperature", AIConstants.OPENAI_TEMPERATURE);

            logger.debug("Making OpenAI Responses API call - Model: {}, Schema: {}", request.get("model"), AIConstants.SCHEMA_NAME);

            Map<String, Object> response = webClient.post()
                    .uri("/responses")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .retryWhen(Retry.backoff(AIConstants.RETRY_MAX_ATTEMPTS, Duration.ofSeconds(AIConstants.RETRY_INITIAL_DELAY_SECONDS))
                            .maxBackoff(Duration.ofSeconds(AIConstants.RETRY_MAX_BACKOFF_SECONDS))
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
                return OpenAIServiceResult.failure("Failed to get response from OpenAI after retries");
            }

            String assistantResponse = null;
            int parsePathUsed = 0; // 1: output_text, 2: output arrays, 3: choices, 4: top-level text, 5: diagnostics
            try {

                if (assistantResponse == null) {
                    Object output = response.get("output");
                    if (output instanceof List) {
                        List<?> outputList = (List<?>) output;
                        StringBuilder aggregated = new StringBuilder();
                        for (Object item : outputList) {
                            if (!(item instanceof Map)) continue;
                            Map<?, ?> itemMap = (Map<?, ?>) item;
                            // Older shape via message -> content
                            Object message = itemMap.get("message");
                            if (message instanceof Map) {
                                Map<?, ?> messageMap = (Map<?, ?>) message;
                                Object content = messageMap.get("content");
                                if (content instanceof List) {
                                    for (Object c : (List<?>) content) {
                                        if (!(c instanceof Map)) continue;
                                        Map<?, ?> cMap = (Map<?, ?>) c;
                                        Object textPart = cMap.get("text");
                                        if (textPart instanceof Map) {
                                            Object value = ((Map<?, ?>) textPart).get("value");
                                            if (value instanceof String) {
                                                aggregated.append((String) value);
                                            }
                                        }
                                        // Some variants may return { type: "output_text", text: "..." }
                                        Object directText = cMap.get("text");
                                        if (directText instanceof String) {
                                            aggregated.append((String) directText);
                                        }
                                    }
                                }
                            }

                            // Newer shape may put content directly on the item: { content: [...] }
                            Object itemContent = itemMap.get("content");
                            if (itemContent instanceof List) {
                                for (Object c : (List<?>) itemContent) {
                                    if (!(c instanceof Map)) continue;
                                    Map<?, ?> cMap = (Map<?, ?>) c;
                                    Object t = cMap.get("text");
                                    if (t instanceof String) {
                                        aggregated.append((String) t);
                                    } else if (t instanceof Map) {
                                        Object val = ((Map<?, ?>) t).get("value");
                                        if (val instanceof String) {
                                            aggregated.append((String) val);
                                        }
                                    }
                                }
                            }
                        }
                        if (aggregated.length() > 0) {
                            assistantResponse = aggregated.toString();
                            parsePathUsed = 2;
                        }
                    }
                }
            } catch (Exception parseEx) {
                logger.error("Error parsing OpenAI response structure: {}", parseEx.getMessage(), parseEx);
            }

            if (assistantResponse == null) {
                Object status = null;
                Object error = null;
                try {
                    status = ((Map<?, ?>) response).get("status");
                    error = ((Map<?, ?>) response).get("error");
                } catch (Exception ignored) {}

                StringBuilder err = new StringBuilder("OpenAI returned empty response");
                if (status != null) err.append(" (status=" + status + ")");
                if (error != null) err.append(" (error=" + error + ")");
                return OpenAIServiceResult.failure(err.toString());
            }
            
            logger.debug("Received OpenAI response ({} chars), is JSON: {}",
                        assistantResponse.length(), assistantResponse.trim().startsWith("{"));

            // Check if we got JSON - if not, return failure
            if (!assistantResponse.trim().startsWith("{")) {
                logger.error("OpenAI returned plain text instead of structured JSON. Response: {}", assistantResponse);
                return OpenAIServiceResult.failure("OpenAI failed to return structured data. Received plain text response instead of JSON.");
            }

            return OpenAIServiceResult.success(assistantResponse);
            
        } catch (WebClientResponseException e) {
            String errorBody = e.getResponseBodyAsString();
            logger.error("OpenAI API Error - Status: " + e.getStatusCode() +
                        ", Body: " + errorBody + ", Message: " + e.getMessage());
            return OpenAIServiceResult.failure("OpenAI API Error " + e.getStatusCode() + ": " + errorBody);
        } catch (Exception e) {
            logger.error("Unexpected error calling OpenAI: {}", e.getMessage(), e);
            return OpenAIServiceResult.failure("Error: " + e.getMessage());
        }
    }

    /**
     * Create response format for structured output
     */
    private Map<String, Object> createTextFormat(List<String> channelKeys) {
        try {
            Map<String, Object> schemaDefinition = createSchemaDefinition(channelKeys);

            Map<String, Object> format = new HashMap<>();
            format.put("type", "json_schema");
            format.put("name", AIConstants.SCHEMA_NAME);
            format.put("strict", AIConstants.STRICT_SCHEMA);
            format.put("schema", schemaDefinition);
            return format;
        } catch (Exception e) {
            logger.error("Error creating ResponseFormat: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Create JSON schema definition for structured output
     */
    private Map<String, Object> createSchemaDefinition(List<String> channelKeys) {
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
        

        
        // Channels property - dynamic structure based on requested channels
        Map<String, Object> channelsProp = new HashMap<>();
        channelsProp.put("type", "object");
        channelsProp.put("additionalProperties", false);
        
        // Define properties for specific channels (only those requested)
        Map<String, Object> channelProperties = new HashMap<>();
        
        // Create idea array definition
        Map<String, Object> ideaArray = new HashMap<>();
        ideaArray.put("type", "array");
        ideaArray.put("minItems", AIConstants.IDEA_MIN_ITEMS);
        ideaArray.put("maxItems", AIConstants.IDEA_MAX_ITEMS);
        
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
        
        // Add only requested channels as properties
        for (String key : channelKeys) {
            channelProperties.put(key, ideaArray);
        }
        channelsProp.put("properties", channelProperties);
        channelsProp.put("required", channelKeys);
        
        properties.put("channels", channelsProp);
        
        schema.put("properties", properties);
        schema.put("required", Arrays.asList("status", "summary", "channels"));
        
        return schema;
    }

    // Normalize requested channels to canonical keys used by schema (e.g., "instagram", "linkedin", "X")
    private List<String> normalizeChannels(List<String> channels) {
        List<String> mapped = new ArrayList<>();
        for (String ch : channels) {
            try {
                mapped.add(com.buffer.enums.ChannelType.fromString(ch).getValue());
            } catch (IllegalArgumentException e) {
                logger.warn("Ignoring unknown channel: {}", ch);
            }
        }
        // De-duplicate while preserving order
        return new ArrayList<>(new LinkedHashSet<>(mapped));
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