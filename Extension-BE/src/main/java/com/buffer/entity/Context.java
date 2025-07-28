package com.buffer.entity;

import com.buffer.dto.ContextRequest;
import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Data
@Builder
public class Context {
    private String sessionId;
    private String originalContent;
    private String title;
    private String description;
    private String url;
    private List<String> headings;
    private String summary; // AI-generated summary of the analysis
    private LocalDateTime createdAt;
    @Builder.Default
    private List<Channel> channels = new ArrayList<>();
    
    public static Context fromContextRequest(ContextRequest request, String sessionId) {
        return Context.builder()
                .sessionId(sessionId)
                .originalContent(request.getFullText())
                .title(request.getTitle())
                .description(request.getDescription())
                .url(request.getUrl())
                .headings(request.getHeadings())
                .createdAt(LocalDateTime.now())
                .channels(new ArrayList<>())
                .build();
    }
    
    public void addChannel(Channel channel) {
        this.channels.add(channel);
    }
} 