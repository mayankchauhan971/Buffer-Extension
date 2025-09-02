package com.buffer.domain.dto.response;

import com.buffer.domain.entity.SocialMediaChannel;
import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Session Data Response DTO
 *
 * DTO representing detailed information about a specific analysis session.
 * Used by monitoring endpoints to provide comprehensive session details for
 * debugging and tracking purposes.
 */
@Data
@Builder
public class SessionDataResponse {
    private String sessionId;
    private String status;
    private String title;
    private String url;
    private String summary;
    private LocalDateTime createdAt;
    private int channelCount;
    private int totalIdeas;
    private List<SocialMediaChannel> channels;
    private String message;
}
