package com.abhiai.abhiai_backend.config;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConfigurationProperties(prefix = "app.media.s3")
public class S3MediaProperties {

    private S3MediaProvider provider = S3MediaProvider.AWS;
    private String bucket;
    private String region = "us-east-1";
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String sessionToken;
    private boolean pathStyle;
    private boolean chunkedEncoding = true;

    public void validate() {
        if (!StringUtils.hasText(bucket)) {
            throw new IllegalStateException("MEDIA_S3_BUCKET is required when cloud media storage is enabled");
        }
        boolean hasAccessKey = StringUtils.hasText(accessKey);
        boolean hasSecretKey = StringUtils.hasText(secretKey);
        if (hasAccessKey != hasSecretKey) {
            throw new IllegalStateException(
                    "MEDIA_S3_ACCESS_KEY and MEDIA_S3_SECRET_KEY must be configured together");
        }
        if (StringUtils.hasText(sessionToken) && !hasAccessKey) {
            throw new IllegalStateException(
                    "MEDIA_S3_SESSION_TOKEN requires an access key and secret key");
        }
        if (provider == S3MediaProvider.R2) {
            validateR2(hasAccessKey);
        } else if (!StringUtils.hasText(region)) {
            throw new IllegalStateException("MEDIA_S3_REGION is required for AWS S3");
        }
    }

    public String effectiveRegion() {
        return provider == S3MediaProvider.R2 ? "auto" : region;
    }

    public boolean effectivePathStyle() {
        return provider == S3MediaProvider.R2 || pathStyle;
    }

    public boolean effectiveChunkedEncoding() {
        return provider != S3MediaProvider.R2 && chunkedEncoding;
    }

    private void validateR2(boolean hasAccessKey) {
        if (!StringUtils.hasText(endpoint)) {
            throw new IllegalStateException("MEDIA_S3_ENDPOINT is required for Cloudflare R2");
        }
        URI endpointUri;
        try {
            endpointUri = URI.create(endpoint);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("MEDIA_S3_ENDPOINT must be a valid HTTPS URL", exception);
        }
        String host = endpointUri.getHost();
        if (!"https".equalsIgnoreCase(endpointUri.getScheme())
                || host == null
                || !host.endsWith(".r2.cloudflarestorage.com")) {
            throw new IllegalStateException(
                    "Cloudflare R2 endpoint must use https://<account-id>.r2.cloudflarestorage.com");
        }
        if (!hasAccessKey) {
            throw new IllegalStateException("Cloudflare R2 requires explicit S3 API credentials");
        }
    }

    public S3MediaProvider getProvider() { return provider; }
    public void setProvider(S3MediaProvider provider) { this.provider = provider; }
    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getAccessKey() { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
    public boolean isPathStyle() { return pathStyle; }
    public void setPathStyle(boolean pathStyle) { this.pathStyle = pathStyle; }
    public boolean isChunkedEncoding() { return chunkedEncoding; }
    public void setChunkedEncoding(boolean chunkedEncoding) { this.chunkedEncoding = chunkedEncoding; }
}
