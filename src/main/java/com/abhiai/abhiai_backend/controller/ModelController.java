package com.abhiai.abhiai_backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.abhiai.abhiai_backend.dto.ai.ModelOptionResponse;
import com.abhiai.abhiai_backend.service.ModelCatalogService;

@RestController
@RequestMapping("/api/v1/models")
public class ModelController {
    private final ModelCatalogService service;
    public ModelController(ModelCatalogService service) { this.service = service; }
    @GetMapping public ResponseEntity<List<ModelOptionResponse>> getModels() { return ResponseEntity.ok(service.getModels()); }
}
