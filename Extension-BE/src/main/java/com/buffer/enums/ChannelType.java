package com.buffer.enums;

public enum ChannelType {
    LINKEDIN("linkedin"),
    INSTAGRAM("instagram"),
    X("X"),
    FACEBOOK("Facebook");
    
    private final String value;
    
    ChannelType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public static ChannelType fromString(String value) {
        for (ChannelType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown channel type: " + value);
    }
} 