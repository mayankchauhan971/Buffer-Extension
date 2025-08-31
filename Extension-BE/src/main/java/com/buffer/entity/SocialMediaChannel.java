package com.buffer.entity;

import com.buffer.enums.ChannelType;
import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Data
@Builder
public class SocialMediaChannel {
    private String channelId;
    private String sessionId; // FK to ContextSession
    private ChannelType name; // Channel type enum
    private String description;
    private LocalDateTime createdAt;
    @Builder.Default
    private List<ContentIdea> contentIdeas = new ArrayList<>();
    
    public static SocialMediaChannel create(String sessionId, ChannelType name) {
        return SocialMediaChannel.builder()
                .channelId(generateChannelId())
                .sessionId(sessionId)
                .name(name)
                .createdAt(LocalDateTime.now())
                .contentIdeas(new ArrayList<>())
                .build();
    }
    
    // Convenience method for backward compatibility
    public static SocialMediaChannel create(String sessionId, String name) {
        return create(sessionId, ChannelType.fromString(name));
    }
    
    public void addIdea(ContentIdea contentIdea) {
        this.contentIdeas.add(contentIdea);
    }


    /**
     * Generates a unique channelID, this is used to identify the channel in the database
     * 
     * @return a unique channelID
    */
    private static String generateChannelId() {
        return "channel_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
} 