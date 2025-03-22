package com.buffer.model;

import lombok.Data;

@Data
public class ChatResponse {
    private String chatId;
    private String response;
    private String status;
    private String errorMessage;

    public void setResponse(String response) {
        this.response = response;
        if (response != null && response.startsWith("STATUS: ")) {
            String[] parts = response.split("\n", 2);
            this.status = parts[0].replace("STATUS: ", "").trim();
            if (this.status.equals("ERROR") && parts.length > 1) {
                this.errorMessage = parts[1].trim();
            }
        } else {
            this.status = "ERROR";
            this.errorMessage = "Invalid response format from AI";
        }
    }
} 