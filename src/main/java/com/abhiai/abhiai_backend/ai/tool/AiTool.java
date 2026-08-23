package com.abhiai.abhiai_backend.ai.tool;

public interface AiTool {
    String name();
    boolean configured();
    String execute(String input);
}
