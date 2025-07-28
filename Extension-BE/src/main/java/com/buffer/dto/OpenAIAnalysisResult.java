package com.buffer.dto;

/**
 * Result wrapper for OpenAI analysis operations.
 * Provides clean success/failure states and error handling.
 */
public class OpenAIAnalysisResult {
    private final boolean success;
    private final String content;
    private final String errorMessage;

    private OpenAIAnalysisResult(boolean success, String content, String errorMessage) {
        this.success = success;
        this.content = content;
        this.errorMessage = errorMessage;
    }

    public static OpenAIAnalysisResult success(String content) {
        return new OpenAIAnalysisResult(true, content, null);
    }

    public static OpenAIAnalysisResult failure(String errorMessage) {
        return new OpenAIAnalysisResult(false, null, errorMessage);
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

    public String getStatus() {
        return success ? "SUCCESS" : "FAILURE";
    }

    public String getMessage() {
        return success ? "Analysis completed successfully" : errorMessage;
    }
} 