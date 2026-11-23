package com.shopsphere.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Builds the RestClient used to call product-service.
 * The base URL and timeouts come from application.yml, so they can change without touching code.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient productRestClient(
            @Value("${product-service.url}") String baseUrl,
            @Value("${product-service.connect-timeout-ms}") long connectTimeoutMs,
            @Value("${product-service.read-timeout-ms}") long readTimeoutMs) {

        // Connect timeout: how long to wait to open the connection (service down / unreachable).
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();

        // Read timeout: how long to wait for the answer once connected (service slow / hanging).
        // WITHOUT timeouts a slow product-service would hang order-service threads forever.
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
