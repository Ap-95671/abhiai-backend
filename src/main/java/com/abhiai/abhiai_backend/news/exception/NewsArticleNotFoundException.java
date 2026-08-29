package com.abhiai.abhiai_backend.news.exception;

public class NewsArticleNotFoundException extends RuntimeException {
    public NewsArticleNotFoundException() { super("News story not found"); }
}
