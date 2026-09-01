package com.abhiai.abhiai_backend.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.abhiai.abhiai_backend.dto.memory.CreateMemoryRequest;
import com.abhiai.abhiai_backend.dto.memory.MemorySettingsResponse;
import com.abhiai.abhiai_backend.dto.memory.UpdateMemorySettingsRequest;
import com.abhiai.abhiai_backend.dto.memory.UserMemoryResponse;
import com.abhiai.abhiai_backend.security.JwtPrincipal;
import com.abhiai.abhiai_backend.service.AiMemoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/memory")
public class AiMemoryController {

    private final AiMemoryService service;

    public AiMemoryController(AiMemoryService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<MemorySettingsResponse> settings(@AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(service.settings(principal.userId()));
    }

    @PatchMapping
    public ResponseEntity<MemorySettingsResponse> update(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody UpdateMemorySettingsRequest request) {
        return ResponseEntity.ok(service.updateEnabled(principal.userId(), request.enabled()));
    }

    @PostMapping("/items")
    public ResponseEntity<UserMemoryResponse> create(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreateMemoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(principal.userId(), request.content()));
    }

    @DeleteMapping("/items/{memoryId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable UUID memoryId) {
        service.delete(principal.userId(), memoryId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/items")
    public ResponseEntity<Void> clear(@AuthenticationPrincipal JwtPrincipal principal) {
        service.clear(principal.userId());
        return ResponseEntity.noContent().build();
    }
}
