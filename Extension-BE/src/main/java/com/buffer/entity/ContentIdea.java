package com.buffer.entity;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ContentIdea {
    private String ideaId;
    private String channelId; // FK to Channel
    private String description;
    private String rationale;
    private List<String> pros;
    private List<String> cons;
    private LocalDateTime createdAt;
    
    public static ContentIdea create(String channelId, String description, String rationale, List<String> pros, List<String> cons) {
        return ContentIdea.builder()
                .ideaId(generateIdeaId())
                .channelId(channelId)
                .description(description)
                .rationale(rationale)
                .pros(pros)
                .cons(cons)
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    private static String generateIdeaId() {
        return "idea_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
} 