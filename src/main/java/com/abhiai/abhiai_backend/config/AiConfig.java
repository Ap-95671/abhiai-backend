package com.abhiai.abhiai_backend.config;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.abhiai.abhiai_backend.ai.gemini.GeminiProperties;
import com.abhiai.abhiai_backend.ai.groq.GroqProperties;
import com.abhiai.abhiai_backend.ai.ollama.OllamaProperties;
import com.abhiai.abhiai_backend.ai.openai.OpenAiProperties;
import com.abhiai.abhiai_backend.ai.image.GeminiImageGenerationProperties;
import com.abhiai.abhiai_backend.ai.image.CloudflareImageGenerationProperties;
import com.abhiai.abhiai_backend.ai.image.ImageGenerationRoutingProperties;
import com.abhiai.abhiai_backend.ai.ModelProvider;
import com.abhiai.abhiai_backend.ai.provider.AnthropicModelProvider;
import com.abhiai.abhiai_backend.ai.provider.OpenAiCompatibleModelProvider;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties({
        OpenAiProperties.class,
        GeminiProperties.class,
        GroqProperties.class,
        OllamaProperties.class,
        GeminiImageGenerationProperties.class,
        CloudflareImageGenerationProperties.class,
        ImageGenerationRoutingProperties.class,
        AiContextProperties.class,
        WebSearchProperties.class,
        MultiProviderProperties.class
})
public class AiConfig {

    @Bean
    public HttpClient aiHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Bean(name = "aiStreamingExecutor", destroyMethod = "shutdown")
    public ExecutorService aiStreamingExecutor() {
        int threadCount = Math.max(2, Math.min(8, Runtime.getRuntime().availableProcessors()));
        return Executors.newFixedThreadPool(threadCount);
    }

    @Bean ModelProvider anthropicProvider(HttpClient aiHttpClient, ObjectMapper mapper, MultiProviderProperties properties) {
        return new AnthropicModelProvider(aiHttpClient, mapper, properties.provider("anthropic"));
    }
    @Bean ModelProvider xaiProvider(HttpClient aiHttpClient, ObjectMapper mapper, MultiProviderProperties properties) {
        return compatible("xai", aiHttpClient, mapper, properties);
    }
    @Bean ModelProvider deepseekProvider(HttpClient aiHttpClient, ObjectMapper mapper, MultiProviderProperties properties) {
        return compatible("deepseek", aiHttpClient, mapper, properties);
    }
    @Bean ModelProvider mistralProvider(HttpClient aiHttpClient, ObjectMapper mapper, MultiProviderProperties properties) {
        return compatible("mistral", aiHttpClient, mapper, properties);
    }
    @Bean ModelProvider cohereProvider(HttpClient aiHttpClient, ObjectMapper mapper, MultiProviderProperties properties) {
        return compatible("cohere", aiHttpClient, mapper, properties);
    }
    @Bean ModelProvider openrouterProvider(HttpClient aiHttpClient, ObjectMapper mapper, MultiProviderProperties properties) {
        return compatible("openrouter", aiHttpClient, mapper, properties);
    }
    private ModelProvider compatible(String name, HttpClient client, ObjectMapper mapper, MultiProviderProperties properties) {
        return new OpenAiCompatibleModelProvider(name, client, mapper, properties.provider(name));
    }
}
