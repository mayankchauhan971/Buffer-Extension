package com.buffer.service;

import com.buffer.model.OpenAIRequest;
import com.buffer.model.OpenAIResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OpenAIService {
    private final WebClient webClient;
    private final Map<String, List<OpenAIRequest.Message>> chatHistory = new ConcurrentHashMap<>();
    
    private String appContext = "I have a small startup that helps small business collect, manage and analyze their reviews";
    private String targetAudience = "mostly small business owners";
    
    private String getDefaultPrompt() {
        return "Go through this article and act as my content generation " + 
               "assistant to help me ideate on what different types of content can I generate out of this give I'll be " + 
               "publishing to Instagram, Facebook, LinkedIn and content for each channel should cater to audience specific " + 
               "to them. Give me three ideas for each channel, from different angles which you feel will perform the " + 
               "maximum along with why you think they'll perform the best. " + 
               "Context - " + appContext + " " + 
               "Target audience - " + targetAudience;
    }

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

    public String processRequest(String url, String selectedText, String chatId, String prompt) {
        List<OpenAIRequest.Message> messages = chatHistory.computeIfAbsent(chatId, k -> new ArrayList<>());
        
        // For first-time users (no messages in history), add the default prompt
        if (messages.isEmpty()) {
            OpenAIRequest.Message systemMessage = new OpenAIRequest.Message();
            systemMessage.setRole("system");
            systemMessage.setContent(getDefaultPrompt());
            messages.add(systemMessage);
        }
        
        // Add user message with URL and selected text
        OpenAIRequest.Message userMessage = new OpenAIRequest.Message();
        userMessage.setRole("user");
        userMessage.setContent(buildPrompt(url, selectedText));
        messages.add(userMessage);

        // If a custom prompt is provided, add it as a follow-up message
        if (prompt != null && !prompt.trim().isEmpty()) {
            OpenAIRequest.Message customPromptMessage = new OpenAIRequest.Message();
            customPromptMessage.setRole("user");
            customPromptMessage.setContent(prompt);
            messages.add(customPromptMessage);
        }
        
        // Create request
        OpenAIRequest request = new OpenAIRequest();
        request.setMessages(messages);

        // Make API call with retry logic
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
                                return wcre.getStatusCode().value() == 429; // Only retry on rate limit
                            }
                            return false;
                        }))
                .block();

        if (response == null) {
            throw new RuntimeException("Failed to get response from OpenAI after retries");
        }

        String assistantResponse = response.getChoices().get(0).getMessage().getContent();

        // Add assistant message to history
        OpenAIRequest.Message assistantMessage = new OpenAIRequest.Message();
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(assistantResponse);
        messages.add(assistantMessage);
        chatHistory.put(chatId, messages);

        return assistantResponse;
    }

    private String buildPrompt(String url, String selectedText) {
        StringBuilder prompt = new StringBuilder();
        if (url != null && !url.isEmpty()) {
            prompt.append("URL: ").append(url).append("\n");
        }
        if (selectedText != null && !selectedText.isEmpty()) {
            prompt.append("Selected Text: ").append(selectedText).append("\n");
        }
        return prompt.toString();
    }
} 