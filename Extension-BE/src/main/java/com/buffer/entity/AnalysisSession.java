package com.buffer.entity;

import com.buffer.dto.ContentAnalysisRequest;
import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Data
@Builder
public class AnalysisSession {
    private String sessionId;
    private String originalContent;
    private String title;
    private String description;
    private String url;
    private List<String> headings;
    private String summary; // AI-generated summary of the analysis
    private LocalDateTime createdAt;
    @Builder.Default
    private List<SocialMediaChannel> socialMediaChannels = new ArrayList<>();
    
    public static AnalysisSession fromContentAnalysisRequest(ContentAnalysisRequest request, String sessionId) {
        return AnalysisSession.builder()
                .sessionId(sessionId)
                .originalContent(request.getFullText())
                .title(request.getTitle())
                .description(request.getDescription())
                .url(request.getUrl())
                .headings(request.getHeadings())
                .createdAt(LocalDateTime.now())
                .socialMediaChannels(new ArrayList<>())
                .build();
    }
    
    public void addChannel(SocialMediaChannel socialMediaChannel) {
        this.socialMediaChannels.add(socialMediaChannel);
    }
} 