package com.buffer.entity;

import com.buffer.util.IdGenerator;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Content Idea Entity
 *
 * Domain entity representing a single AI-generated social media content idea.
 * Contains detailed description, platform-specific rationale, and pros/cons analysis.
 * Associated with a specific social media channel and designed to provide actionable,
 * ready-to-implement content suggestions for social media strategies.
 */
@Entity
@Table(name = "content_ideas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentIdea {
    @Id
    private String ideaId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id")
    private SocialMediaChannel socialMediaChannel;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(columnDefinition = "TEXT")
    private String rationale;
    
    @ElementCollection
    @CollectionTable(name = "content_idea_pros", joinColumns = @JoinColumn(name = "idea_id"))
    @Column(name = "pro")
    private List<String> pros;
    
    @ElementCollection
    @CollectionTable(name = "content_idea_cons", joinColumns = @JoinColumn(name = "idea_id"))
    @Column(name = "con")
    private List<String> cons;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    public static ContentIdea create(SocialMediaChannel socialMediaChannel, String description, String rationale, List<String> pros, List<String> cons) {
        return ContentIdea.builder()
                .ideaId(IdGenerator.generateIdeaId())
                .socialMediaChannel(socialMediaChannel)
                .description(description)
                .rationale(rationale)
                .pros(pros)
                .cons(cons)
                .createdAt(LocalDateTime.now())
                .build();
    }
} 