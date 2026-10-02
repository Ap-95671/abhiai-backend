package com.abhiai.abhiai_backend.ai.orchestration;

import static com.abhiai.abhiai_backend.ai.orchestration.ModelCapability.*;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ModelRegistry {
    private final List<AiModelDefinition> models;
    private com.abhiai.abhiai_backend.config.MultiProviderProperties overrides = new com.abhiai.abhiai_backend.config.MultiProviderProperties();
    @org.springframework.beans.factory.annotation.Autowired
    public void configure(com.abhiai.abhiai_backend.config.MultiProviderProperties properties) { this.overrides = properties; }

    public ModelRegistry(
            @Value("${app.ai.openai.model:gpt-4.1-mini}") String openAi,
            @Value("${app.ai.gemini.model:gemini-3.5-flash}") String gemini,
            @Value("${app.ai.groq.model:openai/gpt-oss-20b}") String groq,
            @Value("${app.ai.ollama.model:gemma3:4b}") String ollama,
            @Value("${app.ai.anthropic.model:claude-sonnet-4-20250514}") String anthropic,
            @Value("${app.ai.xai.model:grok-4.6}") String xai,
            @Value("${app.ai.deepseek.model:deepseek-v4-flash}") String deepseek,
            @Value("${app.ai.mistral.model:mistral-small-latest}") String mistral,
            @Value("${app.ai.cohere.model:command-a-plus-05-2026}") String cohere,
            @Value("${app.ai.openrouter.model:openrouter/auto}") String openrouter) {
        models = List.of(
            model("openai:" + openAi, "openai", openAi, "OpenAI", "Balanced general, coding, and reasoning model", 128000, EnumSet.of(TEXT, VISION, CODE, REASONING), .92, .75, .50),
            model("gemini:" + gemini, "gemini", gemini, "Gemini", "Fast multimodal model with a large context window", 1000000, EnumSet.of(TEXT, VISION, CODE, REASONING, LONG_CONTEXT, FAST, STREAMING), .88, .90, .78),
            model("groq:" + groq, "groq", groq, "Groq", "Low-latency open model inference", 131072, EnumSet.of(TEXT, CODE, FAST, LOW_COST), .72, .98, .90),
            model("ollama:" + ollama, "ollama", ollama, "Local / Ollama", "Private self-hosted inference", 32768, EnumSet.of(TEXT, LOW_COST), .62, .55, 1.0),
            model("anthropic:" + anthropic, "anthropic", anthropic, "Anthropic Claude", "Strong reasoning, writing, and code", 200000, EnumSet.of(TEXT, VISION, CODE, REASONING, LONG_CONTEXT), .94, .72, .42),
            model("xai:" + xai, "xai", xai, "xAI Grok", "General reasoning and current-events model", 131072, EnumSet.of(TEXT, CODE, REASONING, FAST), .87, .82, .52),
            model("deepseek:" + deepseek, "deepseek", deepseek, "DeepSeek", "Cost-efficient coding and reasoning", 128000, EnumSet.of(TEXT, CODE, REASONING, LOW_COST), .86, .75, .92),
            model("mistral:" + mistral, "mistral", mistral, "Mistral", "Efficient multilingual general model", 128000, EnumSet.of(TEXT, CODE, FAST, LOW_COST), .78, .86, .84),
            model("cohere:" + cohere, "cohere", cohere, "Cohere Command", "Enterprise retrieval and tool-oriented model", 256000, EnumSet.of(TEXT, LONG_CONTEXT), .83, .72, .65),
            model("openrouter:" + openrouter, "openrouter", openrouter, "OpenRouter Auto", "OpenRouter-managed model selection", 128000, EnumSet.of(TEXT, CODE, REASONING), .82, .76, .66),
            new AiModelDefinition("abhena:preview", "abhena", "abhena-preview", "Abhena", "AbhiAI native model — coming soon", 0, EnumSet.of(TEXT, CODE, REASONING), 1, 1, 1, ModelStatus.COMING_SOON)
        );
    }

    private AiModelDefinition model(String id, String provider, String providerModelId, String name, String description,
                                    long context, java.util.Set<ModelCapability> caps, double quality, double speed, double cost) {
        return new AiModelDefinition(id, provider, providerModelId, name, description, context, caps, quality, speed, cost, ModelStatus.AVAILABLE);
    }

    public List<AiModelDefinition> all() { return models.stream().map(this::configuredDefinition).toList(); }
    private AiModelDefinition configuredDefinition(AiModelDefinition model) {
        if (model.status() == ModelStatus.COMING_SOON) return model; // catalog-only placeholder
        var p = overrides.provider(model.provider());
        return new AiModelDefinition(model.id(), model.provider(), model.providerModelId(), model.displayName(), model.description(),
                p.getContextWindow() == null ? model.contextWindow() : p.getContextWindow(),
                p.getCapabilities() == null ? model.capabilities() : p.getCapabilities(),
                p.getQualityScore() == null ? model.qualityScore() : p.getQualityScore(),
                p.getSpeedScore() == null ? model.speedScore() : p.getSpeedScore(),
                p.getCostScore() == null ? model.costScore() : p.getCostScore(),
                p.isEnabled() && model.providerModelId() != null && !model.providerModelId().isBlank() ? model.status() : ModelStatus.UNAVAILABLE);
    }
    public Optional<AiModelDefinition> find(String id) { return all().stream().filter(model -> model.id().equals(id)).findFirst(); }
}
