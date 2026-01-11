package ma.enset.productservice.controller;

import ma.enset.productservice.entities.Product;
import ma.enset.productservice.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('CLIENT')")
    public ResponseEntity<List<Product>> getAllProducts(Authentication auth) {
        logger.info("User {} requested all products", auth.getName());
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CLIENT')")
    public ResponseEntity<Product> getProductById(@PathVariable Long id, Authentication auth) {
        logger.info("User {} requested product {}", auth.getName(), id);
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Product> addProduct(@RequestBody Product product, Authentication auth) {
        logger.info("User {} adding product {}", auth.getName(), product.getName());
        return ResponseEntity.ok(productService.addProduct(product));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product product, Authentication auth) {
        logger.info("User {} updating product {}", auth.getName(), id);
        return ResponseEntity.ok(productService.updateProduct(id, product));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id, Authentication auth) {
        logger.info("User {} deleting product {}", auth.getName(), id);
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/stock")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CLIENT')")
    public ResponseEntity<Boolean> checkStock(@PathVariable Long id, @RequestParam int quantity, Authentication auth) {
        logger.debug("User {} checking stock for product {} quantity {}", auth.getName(), id, quantity);
        return ResponseEntity.ok(productService.checkStock(id, quantity));
    }

    @PutMapping("/{id}/reduce-stock")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CLIENT')")
    public ResponseEntity<Void> reduceStock(@PathVariable Long id, @RequestParam int quantity, Authentication auth) {
        logger.info("User {} reducing stock for product {} by {}", auth.getName(), id, quantity);
        productService.reduceStock(id, quantity);
        return ResponseEntity.ok().build();
    }
}
