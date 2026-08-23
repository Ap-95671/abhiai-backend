package com.abhiai.abhiai_backend.service;

import java.io.*; import java.nio.file.*;
import org.springframework.core.io.*; import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.abhiai.abhiai_backend.config.MediaProperties;
import com.abhiai.abhiai_backend.exception.InvalidMediaException;

@Component
@ConditionalOnProperty(prefix="app.media", name="storage-type", havingValue="local", matchIfMissing=true)
public class LocalMediaStorage implements MediaStorage {
    private final Path root;
    public LocalMediaStorage(MediaProperties properties) { this.root = properties.getStoragePath().toAbsolutePath().normalize(); }
    public void store(String key, InputStream content, long size, String contentType) { try { Files.createDirectories(root); Files.copy(content, resolve(key), StandardCopyOption.REPLACE_EXISTING); } catch(IOException e){ throw new InvalidMediaException("Media could not be stored"); } }
    public Resource load(String key) { Resource resource=new FileSystemResource(resolve(key)); if(!resource.exists()) throw new InvalidMediaException("Stored image is unavailable"); return resource; }
    public void delete(String key) { try { Files.deleteIfExists(resolve(key)); } catch(IOException ignored) {} }
    private Path resolve(String key) { Path path=root.resolve(key).normalize(); if(!path.startsWith(root)) throw new InvalidMediaException("Invalid storage key"); return path; }
}
