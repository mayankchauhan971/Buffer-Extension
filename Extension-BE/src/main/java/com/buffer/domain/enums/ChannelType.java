package com.buffer.domain.enums;

public enum ChannelType {
    LINKEDIN,
    INSTAGRAM,
    X;
    
    public static ChannelType fromString(String value) {
        for (ChannelType type : values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown channel type: " + value);
    }
} 