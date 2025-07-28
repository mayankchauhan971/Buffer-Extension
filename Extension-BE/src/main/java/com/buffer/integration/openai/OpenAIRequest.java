package com.buffer.integration.openai;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@Data
public class OpenAIRequest {
    private String model = "gpt-4o-mini";
    private List<Message> messages;
    
    @JsonProperty("response_format")
    private ResponseFormat responseFormat;
    
    private Double temperature = 0.7;

    @Data
    public static class Message {
        private String role;
        private String content;
    }
} 