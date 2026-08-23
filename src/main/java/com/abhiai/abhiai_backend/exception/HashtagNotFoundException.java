package com.abhiai.abhiai_backend.exception;

public class HashtagNotFoundException extends RuntimeException {
    public HashtagNotFoundException() { super("Hashtag not found"); }
}
