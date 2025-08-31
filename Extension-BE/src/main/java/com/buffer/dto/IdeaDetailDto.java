package com.buffer.dto;

import com.buffer.entity.ContentIdea;
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
    public static IdeaDetailDto fromIdea(ContentIdea contentIdea) {
        return IdeaDetailDto.builder()
                .idea(contentIdea.getDescription())
                .rationale(contentIdea.getRationale())
                .pros(contentIdea.getPros())
                .cons(contentIdea.getCons())
                .build();
    }
} 