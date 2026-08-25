package com.abhiai.abhiai_backend.ai.orchestration;

public enum SelectionMode {
    AUTO, MANUAL;

    public static SelectionMode from(String value) {
        if (value == null || value.isBlank()) return AUTO;
        try { return valueOf(value.toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return AUTO; }
    }
}
