package com.api.meteorologia.app.controller;

import com.api.meteorologia.app.service.WeatherService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@CrossOrigin(origins = "*")
@RestController
public class WeatherController {

    @Autowired
    private WeatherService weatherService;

    @GetMapping("/weather/{city}")
    public Mono<String> getWeather(@PathVariable String city) {
        return weatherService.getCurrentWeather(city);
    }

    @GetMapping("/test")
    public String testing() {
        System.out.println("exito!!");
        return "existo!!!!!";
    }
}
