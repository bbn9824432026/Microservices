package com.shopsphere.order.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * The ONLY class in order-service that knows how to talk to product-service.
 * Keeping the HTTP details here means the rest of the code just calls getProduct(id)
 * (and later lessons can add retries / circuit breakers in one place).
 */
@Component
public class ProductClient {

    private final RestClient restClient;

    public ProductClient(RestClient productRestClient) {
        this.restClient = productRestClient;
    }

    /**
     * Calls GET {product-service}/api/products/{id}.
     *
     * @return the product, or empty if product-service answered 404 (product does not exist)
     * @throws ProductServiceUnavailableException if the service is down, too slow, or returned 5xx
     */
    public Optional<ProductDto> getProduct(Long id) {
        try {
            ProductDto product = restClient.get()
                    .uri("/api/products/{id}", id)
                    .retrieve()
                    .body(ProductDto.class);
            return Optional.ofNullable(product);

        } catch (HttpClientErrorException.NotFound e) {
            // 404 is a normal business answer ("no such product"), not a failure of the service
            return Optional.empty();

        } catch (ResourceAccessException e) {
            // Connection refused (service down) or timeout (service slow)
            throw new ProductServiceUnavailableException("product-service is not reachable: " + e.getMessage(), e);

        } catch (HttpServerErrorException e) {
            // product-service itself failed (500, 503 ...)
            throw new ProductServiceUnavailableException("product-service returned " + e.getStatusCode(), e);

        } catch (HttpClientErrorException e) {
            // Other 4xx (e.g. 400 for a bad id) means OUR request was wrong: a bug on our side, let it surface
            throw e;
        }
    }
}
