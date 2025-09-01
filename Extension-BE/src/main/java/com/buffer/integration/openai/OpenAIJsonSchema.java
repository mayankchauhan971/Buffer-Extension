package com.buffer.integration.openai;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * OpenAI JSON Schema Model
 *
 * Data model representing OpenAI's JSON schema structure for structured output requests.
 * Contains schema definitions and format specifications used when calling OpenAI's API
 * with structured output requirements. Supports flexible schema content with nested
 * objects and various data types for complex AI response formatting.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAIJsonSchema {
    private String type;
    
    // Using Object to handle dynamic JSON schema structures with mixed types (strings, arrays, nested objects)
    // Required for OpenAI's flexible schema format that can contain various JSON Schema definitions
    @JsonProperty("json_schema")
    private Object jsonSchema;

    @Data
    @Builder
    public static class JsonSchemaFormat {
        private String name;
        private boolean strict;
        // Object used for flexible JSON schema content (similar reason as above)
        private Object schema;
    }

    @Data
    public static class JsonSchema {
        private String name;
        private boolean strict;
        private Map<String, Object> schema;
    }
} 