package com.abhiai.abhiai_backend.news.config;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NewsProperties.class)
public class NewsConfig {

    @Bean
    public HttpClient newsHttpClient() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    }

    @Bean
    public Clock newsClock() {
        return Clock.systemUTC();
    }
}
