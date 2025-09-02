package com.buffer.integration.openai;

import java.util.*;

import static com.buffer.domain.dto.common.IdeaDetailDto.*;

/**
 * Builder class for constructing JSON Schema definitions for OpenAI structured outputs.
 * 
 * Provides a fluent API for creating complex JSON schemas with proper type safety
 * and reusable components. Specifically designed for OpenAI's structured output format.
 */
public class JsonSchemaBuilder {
    
    // JSON Schema field constants
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_ENUM = "enum";
    private static final String FIELD_ITEMS = "items";
    private static final String FIELD_PROPERTIES = "properties";
    private static final String FIELD_REQUIRED = "required";
    private static final String FIELD_ADDITIONAL_PROPERTIES = "additionalProperties";
    private static final String FIELD_MIN_ITEMS = "minItems";
    private static final String FIELD_MAX_ITEMS = "maxItems";
    
    // JSON Schema type constants
    private static final String TYPE_OBJECT = "object";
    private static final String TYPE_STRING = "string";
    private static final String TYPE_ARRAY = "array";
    
    private final Map<String, Object> schema;
    private final Map<String, Object> properties;
    private final List<String> required;

    private JsonSchemaBuilder() {
        this.schema = new HashMap<>();
        this.properties = new HashMap<>();
        this.required = new ArrayList<>();
        
        // Set defaults for OpenAI structured output
        this.schema.put(FIELD_TYPE, TYPE_OBJECT);
        this.schema.put(FIELD_ADDITIONAL_PROPERTIES, false);
    }
    
    /**
     * Create a new schema builder
     */
    public static JsonSchemaBuilder create() {
        return new JsonSchemaBuilder();
    }
    
    /**
     * Add a string property with optional enum values
     */
    public JsonSchemaBuilder addStringProperty(String name, boolean required, String... enumValues) {
        Map<String, Object> property = new HashMap<>();
        property.put(FIELD_TYPE, TYPE_STRING);
        
        if (enumValues.length > 0) {
            property.put(FIELD_ENUM, Arrays.asList(enumValues));
        }
        
        properties.put(name, property);
        if (required) {
            this.required.add(name);
        }
        return this;
    }
    
    /**
     * Add a string property (required by default)
     */
    public JsonSchemaBuilder addStringProperty(String name) {
        return addStringProperty(name, true);
    }
    
    /**
     * Add an array property with specified item type
     */
    public JsonSchemaBuilder addArrayProperty(String name, Map<String, Object> itemSchema, boolean required) {
        Map<String, Object> property = new HashMap<>();
        property.put(FIELD_TYPE, TYPE_ARRAY);
        property.put(FIELD_ITEMS, itemSchema);
        
        properties.put(name, property);
        if (required) {
            this.required.add(name);
        }
        return this;
    }
    
    /**
     * Add an array property with string items
     */
    public JsonSchemaBuilder addStringArrayProperty(String name, boolean required) {
        return addArrayProperty(name, Map.of(FIELD_TYPE, TYPE_STRING), required);
    }

    /**
     * Add an object property with dynamic properties
     */
    public JsonSchemaBuilder addObjectProperty(String name, Map<String, Object> objectProperties, 
                                             List<String> requiredFields, boolean required) {
        Map<String, Object> property = new HashMap<>();
        property.put(FIELD_TYPE, TYPE_OBJECT);
        property.put(FIELD_ADDITIONAL_PROPERTIES, false);
        property.put(FIELD_PROPERTIES, objectProperties);
        
        if (requiredFields != null && !requiredFields.isEmpty()) {
            property.put(FIELD_REQUIRED, requiredFields);
        }
        
        properties.put(name, property);
        if (required) {
            this.required.add(name);
        }
        return this;
    }

    public Map<String, Object> build() {
        schema.put(FIELD_PROPERTIES, new HashMap<>(properties));
        schema.put(FIELD_REQUIRED, new ArrayList<>(required));
        return new HashMap<>(schema);
    }
    
    // === Convenience methods for common OpenAI patterns ===
    
    /**
     * Create the standard idea item schema used across channels
     */
    public static Map<String, Object> createIdeaItemSchema() {
        return JsonSchemaBuilder.create()
            .addStringProperty(FIELD_IDEA)
            .addStringProperty(FIELD_RATIONALE)
            .addStringArrayProperty(FIELD_PROS, true)
            .addStringArrayProperty(FIELD_CONS, true)
            .build();
    }
    
    /**
     * Create an array schema for ideas with min/max constraints
     */
    public static Map<String, Object> createIdeaArraySchema(int minItems, int maxItems) {
        Map<String, Object> arraySchema = new HashMap<>();
        arraySchema.put(FIELD_TYPE, TYPE_ARRAY);
        arraySchema.put(FIELD_MIN_ITEMS, minItems);
        arraySchema.put(FIELD_MAX_ITEMS, maxItems);
        arraySchema.put(FIELD_ITEMS, createIdeaItemSchema());
        return arraySchema;
    }
}
