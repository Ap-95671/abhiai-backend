package com.abhiai.abhiai_backend.service;

import java.io.InputStream;
import java.net.URI;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.abhiai.abhiai_backend.config.S3MediaProperties;
import com.abhiai.abhiai_backend.exception.InvalidMediaException;

import jakarta.annotation.PreDestroy;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
@ConditionalOnProperty(prefix = "app.media", name = "storage-type", havingValue = "s3")
public class S3MediaStorage implements MediaStorage {

    private final S3Client client;
    private final String bucket;

    public S3MediaStorage(S3MediaProperties properties) {
        properties.validate();
        S3Configuration serviceConfiguration = S3Configuration.builder()
                .pathStyleAccessEnabled(properties.effectivePathStyle())
                .chunkedEncodingEnabled(properties.effectiveChunkedEncoding())
                .build();
        var builder = S3Client.builder()
                .region(Region.of(properties.effectiveRegion()))
                .serviceConfiguration(serviceConfiguration);

        if (StringUtils.hasText(properties.getEndpoint())) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
        }
        if (StringUtils.hasText(properties.getAccessKey())) {
            if (StringUtils.hasText(properties.getSessionToken())) {
                builder.credentialsProvider(StaticCredentialsProvider.create(AwsSessionCredentials.create(
                        properties.getAccessKey(),
                        properties.getSecretKey(),
                        properties.getSessionToken())));
            } else {
                builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                        properties.getAccessKey(),
                        properties.getSecretKey())));
            }
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        this.client = builder.build();
        this.bucket = properties.getBucket();
    }

    @Override
    public void store(String key, InputStream content, long size, String contentType) {
        try {
            client.putObject(
                    PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
                    RequestBody.fromInputStream(content, size));
        } catch (S3Exception exception) {
            throw new InvalidMediaException("Cloud media storage is unavailable");
        }
    }

    @Override
    public Resource load(String key) {
        try {
            ResponseInputStream<GetObjectResponse> stream = client.getObject(
                    GetObjectRequest.builder().bucket(bucket).key(key).build());
            long contentLength = stream.response().contentLength();
            return new InputStreamResource(stream) {
                @Override public String getFilename() { return key; }
                @Override public long contentLength() { return contentLength; }
            };
        } catch (NoSuchKeyException exception) {
            throw new InvalidMediaException("Stored media is unavailable");
        } catch (S3Exception exception) {
            throw new InvalidMediaException("Cloud media storage is unavailable");
        }
    }

    @Override
    public void delete(String key) {
        try {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (S3Exception ignored) {
            // Deletion is idempotent and must not break cleanup of the owning database row.
        }
    }

    @PreDestroy
    void close() {
        client.close();
    }
}
