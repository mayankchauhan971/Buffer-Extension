package com.buffer.dto.response;

import lombok.Data;
import lombok.Builder;

/**
 * Database Health Response DTO
 *
 * DTO representing the health status and statistics of the database.
 * Contains metrics about stored analysis sessions, total ideas generated, and channel
 * distribution. Used by monitoring endpoints to provide system health insights.
 */
@Data
@Builder
public class DatabaseHealthResponse {
    private String status;
    private int sessionCount;
    private int totalIdeas;
    private int totalChannels;
}
