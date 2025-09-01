package com.buffer.dto.response;

import com.buffer.entity.AnalysisSession;
import lombok.Data;
import lombok.Builder;

import java.util.List;

/**
 * Sessions List Response DTO
 *
 * DTO representing a list of all analysis sessions in the system.
 * Contains the response status, total session count, and complete list of session
 * entities. Used by monitoring endpoints to provide an overview of all stored
 * content analysis sessions for administrative and debugging purposes.
 */
@Data
@Builder
public class SessionsListResponse {
    private String status;
    private int sessionCount;
    private List<AnalysisSession> sessions;
}
