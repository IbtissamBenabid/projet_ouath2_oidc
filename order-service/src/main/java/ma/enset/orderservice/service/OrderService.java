package ma.enset.orderservice.service;

import ma.enset.orderservice.entities.Order;
import ma.enset.orderservice.entities.ProductItem;
import ma.enset.orderservice.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final RestClient restClient;

    public OrderService(OrderRepository orderRepository, RestClient restClient) {
        this.orderRepository = orderRepository;
        this.restClient = restClient;
    }

    private String getBearerToken() {
        JwtAuthenticationToken token = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        if (token != null) {
            return "Bearer " + token.getToken().getTokenValue();
        }
        return null;
    }

    public Order createOrder(Order order, String username) {
        validateOrder(order);
        logger.info("User {} creating order with {} items", username, order.getProductItems().size());

        String authHeader = getBearerToken();

        // Check stock for each product
        for (ProductItem item : order.getProductItems()) {
            Boolean stockAvailable = restClient.get()
                    .uri("/products/{id}/stock?quantity={qty}", item.getProductId(), item.getQuantity())
                    .header("Authorization", authHeader)
                    .retrieve()
                    .body(Boolean.class);
            if (!Boolean.TRUE.equals(stockAvailable)) {
                logger.error("Insufficient stock for product {} requested by user {}", item.getProductId(), username);
                throw new RuntimeException("Insufficient stock for product " + item.getProductId());
            }
        }

        // Calculate total
        double total = order.getProductItems().stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
        order.setAmount(total);
        order.setDate(LocalDate.now());
        order.setStatus("PENDING");
        order.setUserId(username);

        Order savedOrder = orderRepository.save(order);
        logger.info("Order {} created for user {}", savedOrder.getId(), username);

        // Reduce stock
        for (ProductItem item : order.getProductItems()) {
            restClient.put()
                    .uri("/products/{id}/reduce-stock?quantity={qty}", item.getProductId(), item.getQuantity())
                    .header("Authorization", authHeader)
                    .retrieve()
                    .toBodilessEntity();
            logger.debug("Reduced stock for product {} by {} for order {}", item.getProductId(), item.getQuantity(), savedOrder.getId());
        }

        return savedOrder;
    }

    public List<Order> getOrdersByUser(String username) {
        logger.info("Fetching orders for user {}", username);
        return orderRepository.findAll().stream()
                .filter(order -> username.equals(order.getUserId()))
                .collect(Collectors.toList());
    }

    public List<Order> getAllOrders() {
        logger.info("Fetching all orders");
        return orderRepository.findAll();
    }

    public Order getOrderById(Long id) {
        logger.info("Fetching order {}", id);
        return orderRepository.findById(id).orElseThrow(() -> {
            logger.error("Order not found with id: {}", id);
            return new RuntimeException("Order not found");
        });
    }

    private void validateOrder(Order order) {
        if (order.getProductItems() == null || order.getProductItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one product");
        }
        for (ProductItem item : order.getProductItems()) {
            if (item.getProductId() == null) {
                throw new IllegalArgumentException("Product ID cannot be null");
            }
            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }
            if (item.getPrice() <= 0) {
                throw new IllegalArgumentException("Price must be positive");
            }
        }
    }
}