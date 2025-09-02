package com.buffer.domain.dto.common;

import com.buffer.domain.entity.ContentIdea;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

/**
 * Idea Detail DTO
 *
 * DTO representing a single social media content idea with detailed
 * information. Contains the core idea description, platform-specific rationale,
 * and balanced perspective with pros/cons analysis. Used within content analysis
 * responses to provide actionable, platform-optimized content suggestions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdeaDetailDto {
    
    public static final String FIELD_CHANNEL = "channel";
    public static final String FIELD_IDEA = "idea";
    public static final String FIELD_RATIONALE = "rationale";
    public static final String FIELD_PROS = "pros";
    public static final String FIELD_CONS = "cons";
    
    private String channel;
    private String idea;
    private String rationale;
    private List<String> pros;
    private List<String> cons;

    public static IdeaDetailDto fromIdea(ContentIdea contentIdea) {
        return IdeaDetailDto.builder()
                .idea(contentIdea.getDescription())
                .rationale(contentIdea.getRationale())
                .pros(contentIdea.getPros())
                .cons(contentIdea.getCons())
                .build();
    }
} 