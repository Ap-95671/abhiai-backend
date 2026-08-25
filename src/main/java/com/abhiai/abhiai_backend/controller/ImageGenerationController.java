package com.abhiai.abhiai_backend.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.abhiai.abhiai_backend.dto.chat.ChatExchangeResponse;
import com.abhiai.abhiai_backend.dto.chat.GenerateImageRequest;
import com.abhiai.abhiai_backend.security.JwtPrincipal;
import com.abhiai.abhiai_backend.service.ImageGenerationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/conversations/{conversationId}/images")
public class ImageGenerationController {

    private final ImageGenerationService service;

    public ImageGenerationController(ImageGenerationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ChatExchangeResponse> generate(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID conversationId,
            @Valid @RequestBody GenerateImageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.generate(principal.userId(), conversationId, request.prompt()));
    }
}
