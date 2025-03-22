package com.buffer.model;

import lombok.Data;
import java.util.List;
import java.util.Arrays;

@Data
public class ChatRequest {
    private String url;
    private String selectedText;
    private String chatId;
    private String prompt;
    private List<String> channels = Arrays.asList("Instagram", "X", "Facebook");
} 