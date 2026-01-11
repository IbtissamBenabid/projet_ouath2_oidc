package ma.enset.orderservice.model;

public record Product(Long id, String name, String description, Double price, int quantity) {
}
