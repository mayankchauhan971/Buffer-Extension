package com.buffer.domain.dto.common;

/**
 * Result wrapper for OpenAI analysis operations.
 * Provides clean success/failure states and error handling.
 */
public class OpenAIServiceResult {
    private final boolean success;
    private final String content;
    private final String errorMessage;

    private OpenAIServiceResult(boolean success, String content, String errorMessage) {
        this.success = success;
        this.content = content;
        this.errorMessage = errorMessage;
    }

    public static OpenAIServiceResult success(String content) {
        return new OpenAIServiceResult(true, content, null);
    }

    public static OpenAIServiceResult failure(String errorMessage) {
        return new OpenAIServiceResult(false, null, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getContent() {
        return content;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}



