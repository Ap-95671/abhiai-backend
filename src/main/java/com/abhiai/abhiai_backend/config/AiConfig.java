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

@Configuration
@EnableConfigurationProperties({
        OpenAiProperties.class,
        GeminiProperties.class,
        GroqProperties.class,
        OllamaProperties.class,
        GeminiImageGenerationProperties.class,
        AiContextProperties.class,
        WebSearchProperties.class
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
}
