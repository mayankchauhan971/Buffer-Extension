package com.buffer.integration.openai;

import java.util.*;

/**
 * Builder class for constructing JSON Schema definitions for OpenAI structured outputs.
 * 
 * Provides a fluent API for creating complex JSON schemas with proper type safety
 * and reusable components. Specifically designed for OpenAI's structured output format.
 */
public class JsonSchemaBuilder {
    private final Map<String, Object> schema;
    private final Map<String, Object> properties;
    private final List<String> required;

    private JsonSchemaBuilder() {
        this.schema = new HashMap<>();
        this.properties = new HashMap<>();
        this.required = new ArrayList<>();
        
        // Set defaults for OpenAI structured output
        this.schema.put("type", "object");
        this.schema.put("additionalProperties", false);
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
        property.put("type", "string");
        
        if (enumValues.length > 0) {
            property.put("enum", Arrays.asList(enumValues));
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
        property.put("type", "array");
        property.put("items", itemSchema);
        
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
        return addArrayProperty(name, Map.of("type", "string"), required);
    }
    
    /**
     * Add an array property with constraints
     */
    public JsonSchemaBuilder addArrayProperty(String name, Map<String, Object> itemSchema, 
                                            boolean required, int minItems, int maxItems) {
        Map<String, Object> property = new HashMap<>();
        property.put("type", "array");
        property.put("items", itemSchema);
        property.put("minItems", minItems);
        property.put("maxItems", maxItems);
        
        properties.put(name, property);
        if (required) {
            this.required.add(name);
        }
        return this;
    }
    
    /**
     * Add an object property with dynamic properties
     */
    public JsonSchemaBuilder addObjectProperty(String name, Map<String, Object> objectProperties, 
                                             List<String> requiredFields, boolean required) {
        Map<String, Object> property = new HashMap<>();
        property.put("type", "object");
        property.put("additionalProperties", false);
        property.put("properties", objectProperties);
        
        if (requiredFields != null && !requiredFields.isEmpty()) {
            property.put("required", requiredFields);
        }
        
        properties.put(name, property);
        if (required) {
            this.required.add(name);
        }
        return this;
    }
    
    /**
     * Add a custom property with full control
     */
    public JsonSchemaBuilder addProperty(String name, Map<String, Object> propertyDefinition, boolean required) {
        properties.put(name, new HashMap<>(propertyDefinition));
        if (required) {
            this.required.add(name);
        }
        return this;
    }
    
    /**
     * Set additional schema properties
     */
    public JsonSchemaBuilder setAdditionalProperties(boolean allow) {
        schema.put("additionalProperties", allow);
        return this;
    }
    
    /**
     * Build the final schema
     */
    public Map<String, Object> build() {
        schema.put("properties", new HashMap<>(properties));
        schema.put("required", new ArrayList<>(required));
        return new HashMap<>(schema);
    }
    
    // === Convenience methods for common OpenAI patterns ===
    
    /**
     * Create the standard idea item schema used across channels
     */
    public static Map<String, Object> createIdeaItemSchema() {
        return JsonSchemaBuilder.create()
            .addStringProperty("idea")
            .addStringProperty("rationale")
            .addStringArrayProperty("pros", true)
            .addStringArrayProperty("cons", true)
            .build();
    }
    
    /**
     * Create an array schema for ideas with min/max constraints
     */
    public static Map<String, Object> createIdeaArraySchema(int minItems, int maxItems) {
        Map<String, Object> arraySchema = new HashMap<>();
        arraySchema.put("type", "array");
        arraySchema.put("minItems", minItems);
        arraySchema.put("maxItems", maxItems);
        arraySchema.put("items", createIdeaItemSchema());
        return arraySchema;
    }
}
