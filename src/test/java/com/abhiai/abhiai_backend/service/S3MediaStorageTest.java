package com.abhiai.abhiai_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.abhiai.abhiai_backend.exception.InvalidMediaException;
import com.abhiai.abhiai_backend.exception.MediaStorageUnavailableException;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

class S3MediaStorageTest {

    @Test
    void buffersUploadSoTheSdkCanRetryWithARepeatableBody() throws IOException {
        S3Client client = mock(S3Client.class);
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        S3MediaStorage storage = new S3MediaStorage(client, "test-bucket");
        byte[] content = "repeatable upload".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        InputStream source = new FilterInputStream(new ByteArrayInputStream(content)) {
            @Override public boolean markSupported() { return false; }
            @Override public synchronized void reset() throws IOException { throw new IOException("reset unsupported"); }
        };

        storage.store("asset.txt", source, content.length, "text/plain");

        ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
        verify(client).putObject(any(PutObjectRequest.class), bodyCaptor.capture());
        RequestBody body = bodyCaptor.getValue();
        try (InputStream firstRead = body.contentStreamProvider().newStream();
             InputStream retryRead = body.contentStreamProvider().newStream()) {
            assertThat(firstRead.readAllBytes()).isEqualTo(content);
            assertThat(retryRead.readAllBytes()).isEqualTo(content);
        }
    }

    @Test
    void rejectsADeclaredSizeThatDoesNotMatchTheUpload() {
        S3MediaStorage storage = new S3MediaStorage(mock(S3Client.class), "test-bucket");

        assertThatThrownBy(() -> storage.store(
                "asset.txt", new ByteArrayInputStream(new byte[] {1, 2}), 3, "text/plain"))
                .isInstanceOf(InvalidMediaException.class)
                .hasMessageContaining("size");
    }

    @Test
    void reportsSdkConnectionFailuresAsStorageOutages() {
        S3Client client = mock(S3Client.class);
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkClientException.create("connection failed"));
        S3MediaStorage storage = new S3MediaStorage(client, "test-bucket");

        assertThatThrownBy(() -> storage.store(
                "asset.txt", new ByteArrayInputStream(new byte[] {1}), 1, "text/plain"))
                .isInstanceOf(MediaStorageUnavailableException.class)
                .hasMessageContaining("temporarily unavailable");
    }
}
