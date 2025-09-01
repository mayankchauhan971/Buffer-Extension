package com.buffer.service;

import com.buffer.dto.common.OpenAIServiceResult;
import com.buffer.dto.response.AIAnalysisResponseDto;
import com.buffer.entity.*;
import com.buffer.config.AIConstants;
import com.buffer.enums.ChannelType;
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
        log.info("Starting content analysis for session: {} with channels: {}", session.getSessionId(), channels);

        if (session.getOriginalContent() == null || session.getOriginalContent().trim().isEmpty()) {
            log.error("No content to analyze for session: {}", session.getSessionId());
            return OpenAIServiceResult.failure("No content provided for analysis");
        }

        List<String> channelsToUse = (channels != null && !channels.isEmpty()) ? channels : defaultChannels;
        if (channelsToUse.isEmpty()) {
            log.warn("No channels provided, using default channels: {}", defaultChannels);
            channelsToUse = defaultChannels;
        }
        List<String> normalizedChannels = normalizeChannels(channelsToUse);

        log.debug("Analyzing {} characters of content for {} channels",
                session.getOriginalContent().length(), normalizedChannels.size());

        String instructions = getSystemPrompt(normalizedChannels);
        String input = session.getOriginalContent();

        log.debug("Prepared OpenAI responses request, content length: {}, channels: {}",
                input.length(), normalizedChannels);

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

            log.debug("Making OpenAI Responses API call - Model: {}, Schema: {}", request.get("model"), AIConstants.SCHEMA_NAME);

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
                                return false;
                            }))
                    .block();

            if (response == null) {
                return OpenAIServiceResult.failure("Failed to get response from OpenAI after retries");
            }

            String assistantResponse = null;
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
                    status = ((Map<?, ?>) response).get("status");
                    error = ((Map<?, ?>) response).get("error");
                } catch (Exception ignored) {}

                StringBuilder err = new StringBuilder("OpenAI returned empty response");
                if (status != null) err.append(" (status=" + status + ")");
                if (error != null) err.append(" (error=" + error + ")");
                return OpenAIServiceResult.failure(err.toString());
            }
            
            log.debug("Received OpenAI response ({} chars), is JSON: {}",
                        assistantResponse.length(), assistantResponse.trim().startsWith("{"));

            if (!assistantResponse.trim().startsWith("{")) {
                log.error("OpenAI returned plain text instead of structured JSON. Response: {}", assistantResponse);
                return OpenAIServiceResult.failure("OpenAI failed to return structured data. Received plain text response instead of JSON.");
            }
            
            // Check for potentially truncated responses
            if (assistantResponse.length() > 20000) {
                log.warn("Received very long response ({} chars) - checking for truncation", assistantResponse.length());
                String trimmed = assistantResponse.trim();
                if (!trimmed.endsWith("}")) {
                    log.error("Response appears to be truncated - doesn't end with closing brace. Length: {}, Last 100 chars: {}", 
                             assistantResponse.length(), 
                             assistantResponse.substring(Math.max(0, assistantResponse.length() - 100)));
                    return OpenAIServiceResult.failure("Received truncated response from OpenAI. Please try with shorter content or fewer channels.");
                }
            }

            return OpenAIServiceResult.success(assistantResponse);
            
        } catch (WebClientResponseException e) {
            String errorBody = e.getResponseBodyAsString();
            log.error("OpenAI API Error - Status: " + e.getStatusCode() +
                        ", Body: " + errorBody + ", Message: " + e.getMessage());
            return OpenAIServiceResult.failure("OpenAI API Error " + e.getStatusCode() + ": " + errorBody);
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
            format.put("type", "json_schema");
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
                .addStringProperty(AIAnalysisResponseDto.FIELD_STATUS, true, AIConstants.STATUS_VALUES)
                .addStringProperty(AIAnalysisResponseDto.FIELD_SUMMARY)
                .addObjectProperty(AIAnalysisResponseDto.FIELD_CHANNELS, channelProperties, channelKeys, true)
                .build();
                
        } catch (Exception e) {
            log.error("Error creating schema definition: {}", e.getMessage(), e);
            // Fallback to empty schema to prevent API call failure
            return Map.of("type", "object", "additionalProperties", true);
        }
    }

    // Normalize requested channels to canonical keys used by schema (e.g., "instagram", "linkedin", "X")
    private List<String> normalizeChannels(List<String> channels) {
        List<String> mapped = new ArrayList<>();
        for (String ch : channels) {
            try {
                ChannelType channelType = ChannelType.fromString(ch);
                String normalizedName = channelType == ChannelType.X ? "X" : channelType.name().toLowerCase();
                mapped.add(normalizedName);
            } catch (IllegalArgumentException e) {
                log.warn("Ignoring unknown channel: {}", ch);
            }
        }
        // De-duplicate while preserving order
        return new ArrayList<>(new LinkedHashSet<>(mapped));
    }

    private String getSystemPrompt(List<String> channels) {
        return AIConstants.buildSystemPrompt(
            String.join(", ", channels),
            appContext,
            targetAudience
        );
    }

} 