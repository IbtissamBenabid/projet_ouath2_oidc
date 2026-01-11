package ma.enset.orderservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class WebClientConfig {

    @Value("${product.service.url:http://localhost:8081}")
    private String productServiceUrl;

    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder
                .baseUrl(productServiceUrl)
                .build();
    }
}