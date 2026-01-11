package ma.enset.orderservice.controller;

import ma.enset.orderservice.entities.Order;
import ma.enset.orderservice.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Order> createOrder(@RequestBody Order order, Authentication authentication) {
        String username = authentication.getName();
        logger.info("User {} creating order", username);
        try {
            Order created = orderService.createOrder(order, username);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            logger.error("Error creating order for user {}: {}", username, e.getMessage());
            throw e;
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<Order>> getMyOrders(Authentication authentication) {
        String username = authentication.getName();
        logger.info("User {} fetching their orders", username);
        return ResponseEntity.ok(orderService.getOrdersByUser(username));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Order>> getAllOrders(Authentication authentication) {
        logger.info("Admin {} fetching all orders", authentication.getName());
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id, Authentication authentication) {
        logger.info("User {} fetching order {}", authentication.getName(), id);
        return ResponseEntity.ok(orderService.getOrderById(id));
    }
}
