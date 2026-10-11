package com.tech.microservices.order_service.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import feign.RequestInterceptor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;


@Configuration
public class InventoryFeignConfig {

    @Value("${inventory.service.username}")
    private String username;

    @Value("${inventory.service.password}")
    private String password;


     @Bean
    public RequestInterceptor inventoryBasicAuthInterceptor() {
        return template -> {
            String token = Base64.getEncoder()
                    .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));

            template.header(HttpHeaders.AUTHORIZATION, "Basic " + token);
        };
    }
}
