package com.abhiai.abhiai_backend.exception;

public class StoryNotFoundException extends RuntimeException {
    public StoryNotFoundException() { super("Story not found or has expired"); }
}
