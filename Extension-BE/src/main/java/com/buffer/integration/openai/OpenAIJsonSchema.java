package com.buffer.integration.openai;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAIJsonSchema {
    private String type;
    
    @JsonProperty("json_schema")
    private Object jsonSchema; // Changed to Object to accept both types

    @Data
    @Builder
    public static class JsonSchemaFormat {
        private String name;
        private boolean strict;
        private Object schema;
    }

    // Adding the old style JsonSchema class for backward compatibility
    @Data
    public static class JsonSchema {
        private String name;
        private boolean strict;
        private Map<String, Object> schema;
    }
} 