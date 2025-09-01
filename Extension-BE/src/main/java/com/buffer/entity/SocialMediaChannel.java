package com.buffer.entity;

import com.buffer.enums.ChannelType;
import com.buffer.util.IdGenerator;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * Social Media Channel Entity
 *
 * Domain entity representing a social media platform channel (Instagram, LinkedIn, X/Twitter).
 * Contains channel-specific metadata and aggregates multiple content ideas targeted for
 * that platform. Manages the relationship between analysis sessions and platform-specific
 * content strategies, enabling organized idea generation and retrieval.
 */

@Entity
@Table(name = "social_media_channels")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialMediaChannel {
    @Id
    private String channelId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private AnalysisSession analysisSession;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "channel_name")
    private ChannelType name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "socialMediaChannel", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ContentIdea> contentIdeas = new ArrayList<>();
    
    public static SocialMediaChannel create(AnalysisSession analysisSession, ChannelType name) {
        return SocialMediaChannel.builder()
                .channelId(IdGenerator.generateChannelId())
                .analysisSession(analysisSession)
                .name(name)
                .createdAt(LocalDateTime.now())
                .contentIdeas(new ArrayList<>())
                .build();
    }

    public static SocialMediaChannel create(AnalysisSession analysisSession, String name) {
        return create(analysisSession, ChannelType.fromString(name));
    }
    
    public void addIdea(ContentIdea contentIdea) {
        this.contentIdeas.add(contentIdea);
    }

} 