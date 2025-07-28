package com.buffer.dto;

import com.buffer.entity.Idea;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdeaDetailDto {
    private String idea;
    private String rationale;
    private List<String> pros;
    private List<String> cons;
    
    /**
     * Create from domain Idea object
     */
    public static IdeaDetailDto fromIdea(Idea idea) {
        return IdeaDetailDto.builder()
                .idea(idea.getDescription())
                .rationale(idea.getRationale())
                .pros(idea.getPros())
                .cons(idea.getCons())
                .build();
    }
} 