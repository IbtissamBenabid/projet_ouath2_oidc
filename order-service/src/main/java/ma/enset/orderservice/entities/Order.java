package ma.enset.orderservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "customer_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDate date;
    private String status;
    private Double amount;

    @Column(name = "user_id")
    private String userId;

    @ElementCollection
    @CollectionTable(name = "order_product_items", joinColumns = @JoinColumn(name = "order_id"))
    private List<ProductItem> productItems;
}
