package com.abhiai.abhiai_backend.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.media")
public class MediaProperties {
    private Path storagePath = Path.of("uploads");
    private long maxImageBytes = 5 * 1024 * 1024;
    private long maxVideoBytes = 25 * 1024 * 1024;
    private long maxDocumentBytes = 10 * 1024 * 1024;
    private int maxImagesPerPost = 4;
    public Path getStoragePath() { return storagePath; }
    public void setStoragePath(Path storagePath) { this.storagePath = storagePath; }
    public long getMaxImageBytes() { return maxImageBytes; }
    public void setMaxImageBytes(long maxImageBytes) { this.maxImageBytes = maxImageBytes; }
    public long getMaxVideoBytes() { return maxVideoBytes; }
    public void setMaxVideoBytes(long maxVideoBytes) { this.maxVideoBytes = maxVideoBytes; }
    public long getMaxDocumentBytes() { return maxDocumentBytes; }
    public void setMaxDocumentBytes(long maxDocumentBytes) { this.maxDocumentBytes = maxDocumentBytes; }
    public int getMaxImagesPerPost() { return maxImagesPerPost; }
    public void setMaxImagesPerPost(int maxImagesPerPost) { this.maxImagesPerPost = maxImagesPerPost; }
}
