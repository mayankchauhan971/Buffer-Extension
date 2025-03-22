package com.buffer.model;

import lombok.Data;

@Data
public class ChatRequest {
    private String url;
    private String selectedText;
    private String chatId;
    private String prompt;
} 