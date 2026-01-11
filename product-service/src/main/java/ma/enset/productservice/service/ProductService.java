package ma.enset.productservice.service;

import ma.enset.productservice.entities.Product;
import ma.enset.productservice.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAllProducts() {
        logger.info("Fetching all products");
        return productRepository.findAll();
    }

    public Product getProductById(Long id) {
        logger.info("Fetching product with id: {}", id);
        Product product = productRepository.findById(id).orElseThrow(() -> {
            logger.error("Product not found with id: {}", id);
            return new RuntimeException("Product not found");
        });
        return product;
    }

    public Product addProduct(Product product) {
        validateProduct(product);
        logger.info("Adding new product: {}", product.getName());
        return productRepository.save(product);
    }

    public Product updateProduct(Long id, Product product) {
        validateProduct(product);
        logger.info("Updating product with id: {}", id);
        Product existing = getProductById(id);
        existing.setName(product.getName());
        existing.setDescription(product.getDescription());
        existing.setPrice(product.getPrice());
        existing.setQuantity(product.getQuantity());
        return productRepository.save(existing);
    }

    public void deleteProduct(Long id) {
        logger.info("Deleting product with id: {}", id);
        productRepository.deleteById(id);
    }

    public boolean checkStock(Long productId, int quantity) {
        logger.debug("Checking stock for product {} with quantity {}", productId, quantity);
        Product product = getProductById(productId);
        return product.getQuantity() >= quantity;
    }

    public void reduceStock(Long productId, int quantity) {
        logger.info("Reducing stock for product {} by {}", productId, quantity);
        Product product = getProductById(productId);
        if (product.getQuantity() < quantity) {
            logger.error("Insufficient stock for product {}: requested {}, available {}", productId, quantity, product.getQuantity());
            throw new RuntimeException("Insufficient stock");
        }
        product.setQuantity(product.getQuantity() - quantity);
        productRepository.save(product);
    }

    private void validateProduct(Product product) {
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be empty");
        }
        if (product.getPrice() == null || product.getPrice() <= 0) {
            throw new IllegalArgumentException("Product price must be positive");
        }
        if (product.getQuantity() < 0) {
            throw new IllegalArgumentException("Product quantity cannot be negative");
        }
    }
}