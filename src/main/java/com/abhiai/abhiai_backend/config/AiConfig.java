package com.abhiai.abhiai_backend.config;

import java.net.http.HttpClient;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.abhiai.abhiai_backend.ai.openai.OpenAiProperties;

@Configuration
@EnableConfigurationProperties(OpenAiProperties.class)
public class AiConfig {

    @Bean
    public HttpClient openAiHttpClient(OpenAiProperties openAiProperties) {
        return HttpClient.newBuilder()
                .connectTimeout(openAiProperties.getConnectTimeout())
                .build();
    }
}
