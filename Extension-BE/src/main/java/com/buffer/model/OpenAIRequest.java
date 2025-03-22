package com.buffer.model;

import lombok.Data;
import java.util.List;

@Data
public class OpenAIRequest {
    private String model = "gpt-3.5-turbo";
    private List<Message> messages;

    @Data
    public static class Message {
        private String role;
        private String content;
    }
} 