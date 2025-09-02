package com.buffer.service;

import com.buffer.domain.dto.common.OpenAIServiceResult;
import com.buffer.domain.dto.response.OpenAIAnalysisDto;
import com.buffer.domain.entity.*;
import com.buffer.web.config.AIConstants;

import com.buffer.util.IdGenerator;
import com.buffer.integration.openai.JsonSchemaBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.core.ParameterizedTypeReference;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.*;

/**
 * OpenAI Integration Service
 *
 * Handles all interactions with OpenAI's API for content analysis and idea generation.
 * Manages structured JSON responses, retry logic, and provides AI-powered content insights.
 * Converts web content into platform-specific social media strategies using GPT models
 * with customizable business context and target audience parameters.
 */

@Slf4j
@Service
public class OpenAIService {
    
    // API endpoint constants
    private static final String RESPONSES_ENDPOINT = "/responses";

    // Request/Response field constants
    private static final String FIELD_MODEL = "model";
    private static final String FIELD_INSTRUCTIONS = "instructions";
    private static final String FIELD_INPUT = "input";
    private static final String FIELD_TEXT = "text";
    private static final String FIELD_FORMAT = "format";
    private static final String FIELD_TEMPERATURE = "temperature";
    private static final String FIELD_OUTPUT = "output";
    private static final String FIELD_MESSAGE = "message";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_VALUE = "value";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_ERROR = "error";

    // Response format constants
    private static final String JSON_SCHEMA_TYPE = "json_schema";

    // Error messages
    private static final String ERROR_EMPTY_RESPONSE = "Failed to get response from OpenAI after retries";
    private static final String ERROR_PLAIN_TEXT_RESPONSE = "OpenAI failed to return structured data. Received plain text response instead of JSON.";

    // HTTP status codes for retry logic
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_REQUEST_TIMEOUT = 408;
    private static final int HTTP_SERVER_ERROR_THRESHOLD = 500;

    private final WebClient webClient;

    // TODO Can be extracted from business context stored at Buffer
    private String appContext = AIConstants.BUSINESS_CONTEXT;
    private String targetAudience = AIConstants.TARGET_AUDIENCE;
    
    private List<String> defaultChannels = AIConstants.DEFAULT_CHANNELS;
    
    public OpenAIService(@Value("${openai.api.key}") String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl(AIConstants.OPENAI_BASE_URL)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public String generateChatId() {
        return IdGenerator.generateChatId();
    }

    public OpenAIServiceResult analyzeContentForIdeas(AnalysisSession session) {
        return analyzeContentForIdeas(session, defaultChannels);
    }

    public OpenAIServiceResult analyzeContentForIdeas(AnalysisSession session, List<String> channels) {

        if (session.getOriginalContent() == null || session.getOriginalContent().trim().isEmpty()) {
            return OpenAIServiceResult.failure("No content provided for analysis");
        }

        List<String> channelsToUse = (channels != null && !channels.isEmpty()) ? channels : defaultChannels;

        // capitalization for consistent handling
        List<String> capitalizedChannels = new ArrayList<>();
        for (String channel : channelsToUse) {
            if (channel != null && !channel.trim().isEmpty()) {
                capitalizedChannels.add(channel.trim().toUpperCase());
            }
        }
        // Remove duplicates while preserving order
        List<String> uniqueChannels = new ArrayList<>(new LinkedHashSet<>(capitalizedChannels));

        String instructions = getSystemPrompt(uniqueChannels);
        String input = session.getOriginalContent();

        return callOpenAIWithStructuredOutput(instructions, input, createTextFormat(uniqueChannels), uniqueChannels);
    }

    /**
     * Make API call with structured JSON output
     */
    private OpenAIServiceResult callOpenAIWithStructuredOutput(String instructions, String input, Map<String, Object> textFormat, List<String> channels) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put(FIELD_MODEL, AIConstants.OPENAI_MODEL);
            request.put(FIELD_INSTRUCTIONS, instructions);
            request.put(FIELD_INPUT, input);
            Map<String, Object> textOptions = new HashMap<>();
            textOptions.put(FIELD_FORMAT, textFormat);
            request.put(FIELD_TEXT, textOptions);
            request.put(FIELD_TEMPERATURE, AIConstants.OPENAI_TEMPERATURE);

            Map<String, Object> response = webClient.post()
                    .uri(RESPONSES_ENDPOINT)
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
                                    return statusCode == HTTP_TOO_MANY_REQUESTS || // Rate limit
                                           statusCode >= HTTP_SERVER_ERROR_THRESHOLD || // Server errors
                                           statusCode == HTTP_REQUEST_TIMEOUT;   // Request timeout
                                }
                                return false;
                            }))
                    .block();

            if (response == null) {
                return OpenAIServiceResult.failure(ERROR_EMPTY_RESPONSE);
            }

            String assistantResponse = null;
            try {

                if (assistantResponse == null) {
                    Object output = response.get(FIELD_OUTPUT);
                    if (output instanceof List) {
                        List<?> outputList = (List<?>) output;
                        StringBuilder aggregated = new StringBuilder();
                        for (Object item : outputList) {
                            if (!(item instanceof Map)) continue;
                            Map<?, ?> itemMap = (Map<?, ?>) item;
                            // Older shape via message -> content
                            Object message = itemMap.get(FIELD_MESSAGE);
                            if (message instanceof Map) {
                                Map<?, ?> messageMap = (Map<?, ?>) message;
                                Object content = messageMap.get(FIELD_CONTENT);
                                if (content instanceof List) {
                                    for (Object c : (List<?>) content) {
                                        if (!(c instanceof Map)) continue;
                                        Map<?, ?> cMap = (Map<?, ?>) c;
                                        Object textPart = cMap.get(FIELD_TEXT);
                                        if (textPart instanceof Map) {
                                            Object value = ((Map<?, ?>) textPart).get(FIELD_VALUE);
                                            if (value instanceof String) {
                                                aggregated.append((String) value);
                                            }
                                        }
                                        // Some variants may return { type: "output_text", text: "..." }
                                        Object directText = cMap.get(FIELD_TEXT);
                                        if (directText instanceof String) {
                                            aggregated.append((String) directText);
                                        }
                                    }
                                }
                            }

                            // Newer shape may put content directly on the item: { content: [...] }
                            Object itemContent = itemMap.get(FIELD_CONTENT);
                            if (itemContent instanceof List) {
                                for (Object c : (List<?>) itemContent) {
                                    if (!(c instanceof Map)) continue;
                                    Map<?, ?> cMap = (Map<?, ?>) c;
                                    Object t = cMap.get(FIELD_TEXT);
                                    if (t instanceof String) {
                                        aggregated.append((String) t);
                                    } else if (t instanceof Map) {
                                        Object val = ((Map<?, ?>) t).get(FIELD_VALUE);
                                        if (val instanceof String) {
                                            aggregated.append((String) val);
                                        }
                                    }
                                }
                            }
                        }
                        if (aggregated.length() > 0) {
                            assistantResponse = aggregated.toString();
                        }
                    }
                }
            } catch (Exception parseEx) {
                log.error("Error parsing OpenAI response structure: {}", parseEx.getMessage(), parseEx);
            }

            if (assistantResponse == null) {
                Object status = null;
                Object error = null;
                try {
                    status = ((Map<?, ?>) response).get(FIELD_STATUS);
                    error = ((Map<?, ?>) response).get(FIELD_ERROR);
                } catch (Exception ignored) {}

                StringBuilder err = new StringBuilder("OpenAI returned empty response");
                if (status != null) err.append(" (status=" + status + ")");
                if (error != null) err.append(" (error=" + error + ")");
                return OpenAIServiceResult.failure(err.toString());
            }

            if (!assistantResponse.trim().startsWith("{")) {
                log.error("OpenAI returned plain text instead of structured JSON. Response: {}", assistantResponse);
                return OpenAIServiceResult.failure(ERROR_PLAIN_TEXT_RESPONSE);
            }

            return OpenAIServiceResult.success(assistantResponse);

        } catch (Exception e) {
            log.error("Unexpected error calling OpenAI: {}", e.getMessage(), e);
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
            format.put("type", JSON_SCHEMA_TYPE);
            format.put("name", AIConstants.SCHEMA_NAME);
            format.put("strict", AIConstants.STRICT_SCHEMA);
            format.put("schema", schemaDefinition);
            return format;
        } catch (Exception e) {
            log.error("Error creating ResponseFormat: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Create JSON schema definition for structured output using builder pattern
     */
    private Map<String, Object> createSchemaDefinition(List<String> channelKeys) {
        try {
            Map<String, Object> ideaArraySchema = JsonSchemaBuilder.createIdeaArraySchema(
                AIConstants.IDEA_MIN_ITEMS, 
                AIConstants.IDEA_MAX_ITEMS
            );
            
            Map<String, Object> channelProperties = new HashMap<>();
            for (String channelKey : channelKeys) {
                channelProperties.put(channelKey, ideaArraySchema);
            }
            
            return JsonSchemaBuilder.create()
                .addStringProperty(OpenAIAnalysisDto.FIELD_STATUS, true, AIConstants.STATUS_VALUES)
                .addStringProperty(OpenAIAnalysisDto.FIELD_SUMMARY)
                .addObjectProperty(OpenAIAnalysisDto.FIELD_CHANNELS, channelProperties, channelKeys, true)
                .build();
                
        } catch (Exception e) {
            log.error("Error creating schema definition: {}", e.getMessage(), e);
            return Map.of("type", "object", "additionalProperties", true);
        }
    }

    private String getSystemPrompt(List<String> channels) {
        return AIConstants.buildSystemPrompt(
            String.join(", ", channels),
            appContext,
            targetAudience
        );
    }

} 