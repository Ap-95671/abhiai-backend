package com.abhiai.abhiai_backend.news.exception;

public class NewsProviderException extends RuntimeException {
    public NewsProviderException(String message) { super(message); }
    public NewsProviderException(String message, Throwable cause) { super(message, cause); }
}
