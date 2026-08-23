package com.abhiai.abhiai_backend.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class S3MediaPropertiesTest {

    @Test
    void acceptsAwsWithTheDefaultCredentialChain() {
        S3MediaProperties properties = new S3MediaProperties();
        properties.setBucket("abhiai-media");
        properties.setRegion("ap-south-1");

        assertDoesNotThrow(properties::validate);
        assertEquals("ap-south-1", properties.effectiveRegion());
        assertFalse(properties.effectivePathStyle());
        assertTrue(properties.effectiveChunkedEncoding());
    }

    @Test
    void enforcesCloudflareR2TransportSettings() {
        S3MediaProperties properties = r2Properties();

        assertDoesNotThrow(properties::validate);
        assertEquals("auto", properties.effectiveRegion());
        assertTrue(properties.effectivePathStyle());
        assertFalse(properties.effectiveChunkedEncoding());
    }

    @Test
    void rejectsPartialCredentialsAndSessionTokensWithoutKeys() {
        S3MediaProperties properties = new S3MediaProperties();
        properties.setBucket("abhiai-media");
        properties.setAccessKey("access");
        assertThrows(IllegalStateException.class, properties::validate);

        properties.setAccessKey(null);
        properties.setSessionToken("temporary");
        assertThrows(IllegalStateException.class, properties::validate);
    }

    @Test
    void rejectsR2WithoutItsOfficialEndpointOrExplicitCredentials() {
        S3MediaProperties properties = r2Properties();
        properties.setEndpoint("https://example.com");
        assertThrows(IllegalStateException.class, properties::validate);

        properties = r2Properties();
        properties.setSecretKey(null);
        assertThrows(IllegalStateException.class, properties::validate);
    }

    @Test
    void acceptsExplicitHttpsS3CompatibleProviderConfiguration() {
        S3MediaProperties properties = compatibleProperties();

        assertDoesNotThrow(properties::validate);
        assertEquals("ap-northeast-2", properties.effectiveRegion());
        assertTrue(properties.effectivePathStyle());
        assertFalse(properties.effectiveChunkedEncoding());
    }

    @Test
    void rejectsS3CompatibleProviderWithoutHttpsEndpointOrCredentials() {
        S3MediaProperties properties = compatibleProperties();
        properties.setEndpoint("http://storage.example.com");
        assertThrows(IllegalStateException.class, properties::validate);

        properties = compatibleProperties();
        properties.setAccessKey(null);
        assertThrows(IllegalStateException.class, properties::validate);
    }

    private S3MediaProperties r2Properties() {
        S3MediaProperties properties = new S3MediaProperties();
        properties.setProvider(S3MediaProvider.R2);
        properties.setBucket("abhiai-media");
        properties.setEndpoint("https://account-id.r2.cloudflarestorage.com");
        properties.setAccessKey("access");
        properties.setSecretKey("secret");
        return properties;
    }

    private S3MediaProperties compatibleProperties() {
        S3MediaProperties properties = new S3MediaProperties();
        properties.setProvider(S3MediaProvider.S3_COMPATIBLE);
        properties.setBucket("abhiai-media");
        properties.setRegion("ap-northeast-2");
        properties.setEndpoint("https://project.storage.supabase.co/storage/v1/s3");
        properties.setAccessKey("access");
        properties.setSecretKey("secret");
        properties.setPathStyle(true);
        properties.setChunkedEncoding(false);
        return properties;
    }
}
