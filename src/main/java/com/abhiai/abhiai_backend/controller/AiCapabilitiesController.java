package com.abhiai.abhiai_backend.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.abhiai.abhiai_backend.ai.AiProvider;
import com.abhiai.abhiai_backend.dto.ai.AiCapabilitiesResponse;
import com.abhiai.abhiai_backend.ai.tool.AiToolRegistry;

@RestController
@RequestMapping("/api/v1/ai")
public class AiCapabilitiesController {

    private final AiProvider provider;
    private final AiToolRegistry tools;

    public AiCapabilitiesController(AiProvider provider, AiToolRegistry tools) {
        this.provider = provider;
        this.tools = tools;
    }

    @GetMapping("/capabilities")
    public AiCapabilitiesResponse capabilities() {
        return new AiCapabilitiesResponse(
                provider.providerName(),
                provider.modelName(),
                provider.configured(),
                Map.ofEntries(
                        Map.entry("textChat", true),
                        Map.entry("streaming", true),
                        Map.entry("conversationContext", true),
                        Map.entry("conversationAttachments", true),
                        Map.entry("imageGeneration", false),
                        Map.entry("imageUnderstanding", provider.supportsImageUnderstanding()),
                        Map.entry("documentAnalysis", true),
                        Map.entry("scannedDocumentOcr", true),
                        Map.entry("retrievalAugmentedGeneration", true),
                        Map.entry("webSearch", tools.webSearchConfigured()),
                        Map.entry("toolCalling", true),
                        Map.entry("longTermMemory", false)));
    }
}
