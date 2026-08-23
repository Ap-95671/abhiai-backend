package com.abhiai.abhiai_backend.service;
import java.io.InputStream;
import org.springframework.core.io.Resource;
public interface MediaStorage { void store(String key, InputStream content, long size, String contentType); Resource load(String key); void delete(String key); }
