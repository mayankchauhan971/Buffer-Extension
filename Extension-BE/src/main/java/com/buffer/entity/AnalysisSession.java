package com.buffer.entity;

import com.buffer.dto.request.ContentAnalysisRequest;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * Analysis Session Entity
 *
 * Domain entity representing a complete content analysis session. Stores original
 * webpage content, metadata, AI-generated summary, and associated social media channels
 * with their content ideas. Serves as the root aggregate for content analysis workflows
 * and provides session-based tracking and retrieval capabilities.
 */

@Entity
@Table(name = "analysis_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisSession {
    @Id
    private String sessionId;
    
    @Column(columnDefinition = "TEXT")
    private String originalContent;
    
    @Column(length = 500)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(length = 1000)
    private String url;
    
    @ElementCollection
    @CollectionTable(name = "session_headings", joinColumns = @JoinColumn(name = "session_id"))
    @Column(name = "heading")
    private List<String> headings;
    
    @Column(columnDefinition = "TEXT")
    private String summary;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "analysisSession", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
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