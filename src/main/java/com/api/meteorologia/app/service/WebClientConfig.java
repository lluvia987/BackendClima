package com.api.meteorologia.app.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl("https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
