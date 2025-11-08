package com.api.meteorologia.app.service;

import com.api.meteorologia.app.common.exception.BusinessException;
import com.api.meteorologia.app.common.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    private static final String SERVICE_NAME = "WeatherAPI";
    private static final String CACHE_KEY_PREFIX = "weather:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);
    private static final Duration API_TIMEOUT = Duration.ofSeconds(10);

    private final WebClient webClient;
    private final RedisTemplate<String, String> redisTemplate;

    public WeatherService(WebClient webClient, RedisTemplate<String, String> redisTemplate) {
        this.webClient = webClient;
        this.redisTemplate = redisTemplate;
    }

    public Mono<String> getCurrentWeather(String city) {
        log.info("Requesting weather data for city: {}", city);

        String cacheKey = CACHE_KEY_PREFIX + city;

        return getFromCache(cacheKey)
                .switchIfEmpty(fetchFromApi(city, cacheKey))
                .doOnSuccess(data -> log.info("Successfully retrieved weather for: {}", city))
                .doOnError(error -> log.error("Failed to get weather for: {}", city, error));
    }

    private Mono<String> getFromCache(String key) {
        return Mono.fromCallable(() -> redisTemplate.opsForValue().get(key))
                .doOnNext(cached -> {
                    if (cached != null) {
                        log.debug("Cache HIT: {}", key);
                    }
                })
                .onErrorResume(error -> {
                    log.warn("Cache read failed: {}", error.getMessage());
                    return Mono.empty(); // Continuar sin cache
                });
    }

    private Mono<String> fetchFromApi(String city, String cacheKey) {
        log.info("Fetching from API for city: {}", city);

        return webClient.get()
                .uri("/{city}/today?unitGroup=us&elements=datetime,datetimeEpoch,tempmax,tempmin,temp,feelslikemax&key=TLEV42DZ26BQA4FBRQN8KNEX8&contentType=json", city)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError(),
                        response -> {
                            log.error("API returned 4xx for city: {}. Status: {}", city, response.statusCode());
                            return Mono.error(new BusinessException(
                                    "Invalid city or request parameters",
                                    "INVALID_REQUEST",
                                    HttpStatus.BAD_REQUEST
                            ));
                        }
                )
                .onStatus(
                        status -> status.is5xxServerError(),
                        response -> {
                            log.error("API returned 5xx for city: {}. Status: {}", city, response.statusCode());
                            return Mono.error(new ExternalServiceException(
                                    "Weather service is experiencing issues",
                                    SERVICE_NAME
                            ));
                        }
                )
                .bodyToMono(String.class)
                .timeout(API_TIMEOUT)
                .doOnNext(response -> saveToCache(cacheKey, response))
                .onErrorMap(TimeoutException.class, ex -> {
                    log.error("Timeout fetching weather for city: {}", city);
                    return new ExternalServiceException(
                            "Weather service timeout",
                            SERVICE_NAME
                    );
                })
                .onErrorMap(WebClientRequestException.class, ex -> {
                    log.error("Network error for city: {}", city, ex);
                    return new ExternalServiceException(
                            "Cannot connect to weather service",
                            SERVICE_NAME
                    );
                });
    }

    private void saveToCache(String key, String value) {
        try {
            redisTemplate.opsForValue().set(key, value, CACHE_TTL);
            log.debug("Cached data with key: {}", key);
        } catch (Exception e) {
            log.warn("Failed to cache data: {}", e.getMessage());
            // No lanzar excepción, el cache es opcional
        }
    }
}