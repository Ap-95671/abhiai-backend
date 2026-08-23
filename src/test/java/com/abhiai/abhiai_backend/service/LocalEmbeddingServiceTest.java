package com.abhiai.abhiai_backend.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LocalEmbeddingServiceTest {

    @Test
    void ranksRelatedTextAboveUnrelatedText() {
        LocalEmbeddingService embeddings = new LocalEmbeddingService();
        String query = "Spring Boot authentication security";

        double related = embeddings.similarity(
                query,
                embeddings.embed("JWT authentication and Spring Security configuration"));
        double unrelated = embeddings.similarity(
                query,
                embeddings.embed("Cooking pasta with tomato and basil"));

        assertTrue(related > unrelated);
    }
}
