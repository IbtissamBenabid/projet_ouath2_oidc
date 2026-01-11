package ma.enset.gateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/dashboard")
public class HealthDashboardController {

    private final WebClient webClient;

    @Value("${PRODUCT_SERVICE_URI:http://localhost:8081}")
    private String productServiceUri;

    @Value("${ORDER_SERVICE_URI:http://localhost:8082}")
    private String orderServiceUri;

    public HealthDashboardController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @GetMapping("/health")
    public Mono<Map<String, Object>> getHealthDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("timestamp", LocalDateTime.now().toString());
        dashboard.put("gateway", "UP");

        Mono<String> productHealth = checkServiceHealth(productServiceUri + "/actuator/health");
        Mono<String> orderHealth = checkServiceHealth(orderServiceUri + "/actuator/health");

        return Mono.zip(productHealth, orderHealth)
                .map(tuple -> {
                    Map<String, Object> services = new HashMap<>();
                    services.put("product-service", parseHealthStatus(tuple.getT1()));
                    services.put("order-service", parseHealthStatus(tuple.getT2()));
                    dashboard.put("services", services);
                    dashboard.put("overallStatus", calculateOverallStatus(services));
                    return dashboard;
                })
                .onErrorReturn(createErrorDashboard());
    }

    @GetMapping("/services")
    public Mono<Map<String, Object>> getServicesStatus() {
        Map<String, Object> result = new HashMap<>();

        Mono<Map<String, Object>> productInfo = getServiceInfo(productServiceUri, "product-service");
        Mono<Map<String, Object>> orderInfo = getServiceInfo(orderServiceUri, "order-service");

        return Mono.zip(productInfo, orderInfo)
                .map(tuple -> {
                    result.put("product-service", tuple.getT1());
                    result.put("order-service", tuple.getT2());
                    result.put("timestamp", LocalDateTime.now().toString());
                    return result;
                });
    }

    private Mono<Map<String, Object>> getServiceInfo(String baseUri, String serviceName) {
        return webClient.get()
                .uri(baseUri + "/actuator/health")
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("name", serviceName);
                    info.put("uri", baseUri);
                    info.put("status", "UP");
                    info.put("health", parseHealthStatus(response));
                    return info;
                })
                .onErrorResume(e -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("name", serviceName);
                    info.put("uri", baseUri);
                    info.put("status", "DOWN");
                    info.put("error", e.getMessage());
                    return Mono.just(info);
                });
    }

    private Mono<String> checkServiceHealth(String healthUrl) {
        return webClient.get()
                .uri(healthUrl)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorReturn("{\"status\":\"DOWN\"}");
    }

    private Map<String, Object> parseHealthStatus(String healthResponse) {
        Map<String, Object> status = new HashMap<>();
        if (healthResponse.contains("\"status\":\"UP\"")) {
            status.put("status", "UP");
            status.put("healthy", true);
        } else {
            status.put("status", "DOWN");
            status.put("healthy", false);
        }
        return status;
    }

    private String calculateOverallStatus(Map<String, Object> services) {
        for (Object service : services.values()) {
            if (service instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> serviceMap = (Map<String, Object>) service;
                if (!"UP".equals(serviceMap.get("status"))) {
                    return "DEGRADED";
                }
            }
        }
        return "UP";
    }

    private Map<String, Object> createErrorDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("timestamp", LocalDateTime.now().toString());
        dashboard.put("gateway", "UP");
        dashboard.put("overallStatus", "ERROR");
        dashboard.put("message", "Unable to fetch service health");
        return dashboard;
    }
}
