package com.buffer.controller;

import com.buffer.model.ChatRequest;
import com.buffer.model.ChatResponse;
import com.buffer.service.OpenAIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    private final OpenAIService openAIService;

    @Autowired
    public ChatController(OpenAIService openAIService) {
        this.openAIService = openAIService;
    }

    @PostMapping("/api/chat")
    public ChatResponse processChat(@RequestBody ChatRequest request) {
        logger.info("Received chat request - URL: {}, SelectedText: {}, ChatId: {}, Prompt: {}, Channels: {}", 
            request.getUrl(), request.getSelectedText(), request.getChatId(), request.getPrompt(), request.getChannels());
        
        String chatId = request.getChatId();
        if (chatId == null || chatId.isEmpty()) {
            chatId = openAIService.generateChatId();
            logger.info("Generated new chatId: {}", chatId);
        }

        String response = openAIService.processRequest(
                request.getUrl(),
                request.getSelectedText(),
                chatId,
                request.getPrompt(),
                request.getChannels()
        );

        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setChatId(chatId);
        chatResponse.setResponse(response);
        
        logger.info("Completed processing chat request with chatId: {}", chatId);
        return chatResponse;
    }
} 