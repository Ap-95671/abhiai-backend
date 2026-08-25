package com.abhiai.abhiai_backend.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.abhiai.abhiai_backend.ai.ModelProvider;
import com.abhiai.abhiai_backend.ai.orchestration.ModelRegistry;
import com.abhiai.abhiai_backend.ai.orchestration.ProviderHealthTracker;
import com.abhiai.abhiai_backend.dto.ai.ModelOptionResponse;

@Service
public class ModelCatalogService {
    private final ModelRegistry registry;
    private final ProviderHealthTracker health;
    private final Map<String, ModelProvider> providers = new LinkedHashMap<>();

    public ModelCatalogService(ModelRegistry registry, ProviderHealthTracker health, List<ModelProvider> providers) {
        this.registry = registry; this.health = health;
        providers.forEach(provider -> this.providers.put(provider.providerName(), provider));
    }

    public List<ModelOptionResponse> getModels() {
        return registry.all().stream().map(model -> {
            ModelProvider provider = providers.get(model.provider());
            boolean configured = provider != null && provider.configured();
            String status = model.status() == com.abhiai.abhiai_backend.ai.orchestration.ModelStatus.COMING_SOON
                    ? model.status().name() : health.status(model.provider(), configured).name();
            return new ModelOptionResponse(model.id(), model.provider(), model.displayName(), model.description(),
                    model.contextWindow(), model.capabilities().stream().map(Enum::name).collect(java.util.stream.Collectors.toUnmodifiableSet()),
                    status, configured);
        }).toList();
    }
}
