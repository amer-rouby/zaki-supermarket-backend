package com.zakisupermarket.controller;

import com.zakisupermarket.dto.request.ProductRequest;
import com.zakisupermarket.dto.response.ApiResponse;
import com.zakisupermarket.dto.response.ProductResponse;
import com.zakisupermarket.service.ProductService;
import com.zakisupermarket.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for managing products within the SmartPharma system.
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts(
            @RequestParam Long storeId) {

        storeId = SecurityUtils.getCurrentStoreId();

        log.info("GET /api/products - storeId: {}", storeId);

        List<ProductResponse> products = productService.getAllProducts(storeId);
        return ResponseEntity.ok(
                ApiResponse.success(products, "Products retrieved successfully")
        );
    }

    /**
     * Real backend-paginated + searchable product listing for the Products management
     * screen. Deliberately a separate endpoint from GET /api/products (above), which
     * intentionally still returns the full unpaginated list - sales-form and the
     * quick-add-scan screen depend on having every product loaded client-side for
     * instant in-memory barcode-exact-match lookups without a network round-trip per
     * scan, so that endpoint's behavior must not change.
     */
    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'VIEWER')")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getProductsPage(
            @RequestParam Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        storeId = SecurityUtils.getCurrentStoreId();

        log.info("GET /api/products/page - storeId: {}, page: {}, size: {}, search: '{}', category: '{}'",
                storeId, page, size, search, category);

        Page<ProductResponse> products = productService.getProductsPage(
                storeId, page, size, search, category, sortBy, sortDirection);
        return ResponseEntity.ok(ApiResponse.success(products, "Products retrieved successfully"));
    }

    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'VIEWER')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getProductsCount(
            @RequestParam Long storeId) {

        storeId = SecurityUtils.getCurrentStoreId();

        log.info("GET /api/products/count - storeId: {}", storeId);

        Long count = productService.getProductsCount(storeId);

        Map<String, Long> response = new HashMap<>();
        response.put("count", count);

        return ResponseEntity.ok(
                ApiResponse.success(response, "Products count retrieved successfully")
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'VIEWER')")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(
            @PathVariable Long id,
            @RequestParam Long storeId) {

        storeId = SecurityUtils.getCurrentStoreId();

        log.info("GET /api/products/{} - storeId: {}", id, storeId);

        ProductResponse product = productService.getProduct(id, storeId);
        return ResponseEntity.ok(
                ApiResponse.success(product, "Product retrieved successfully")
        );
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request,
            @RequestParam Long storeId) {

        storeId = SecurityUtils.getCurrentStoreId();

        log.info("POST /api/products - storeId: {}, productName: {}",
                storeId, request.getName());

        ProductResponse product = productService.createProduct(request, storeId);
        return ResponseEntity.ok(
                ApiResponse.success(product, "Product created successfully")
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request,
            @RequestParam Long storeId) {

        storeId = SecurityUtils.getCurrentStoreId();

        log.info("PUT /api/products/{} - storeId: {}", id, storeId);

        ProductResponse product = productService.updateProduct(id, request, storeId);
        return ResponseEntity.ok(
                ApiResponse.success(product, "Product updated successfully")
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable Long id,
            @RequestParam Long storeId) {

        storeId = SecurityUtils.getCurrentStoreId();

        log.info("DELETE /api/products/{} - storeId: {}", id, storeId);

        productService.deleteProduct(id, storeId);
        return ResponseEntity.ok(
                ApiResponse.success(null, "Product deleted successfully")
        );
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> searchProducts(
            @RequestParam Long storeId,
            @RequestParam String query) {

        storeId = SecurityUtils.getCurrentStoreId();

        log.info("GET /api/products/search - storeId: {}, query: '{}'", storeId, query);

        List<ProductResponse> products = productService.searchProducts(storeId, query);
        return ResponseEntity.ok(
                ApiResponse.success(products, "Search completed successfully")
        );
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getLowStockProducts(
            @RequestParam Long storeId) {

        storeId = SecurityUtils.getCurrentStoreId();

        log.info("GET /api/products/low-stock - storeId: {}", storeId);

        List<ProductResponse> products = productService.getLowStockProducts(storeId);
        return ResponseEntity.ok(
                ApiResponse.success(products, "Low stock products retrieved")
        );
    }

    @PostMapping("/calculate-sell-price")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> calculateSellPrice(
            @RequestParam BigDecimal buyPrice,
            @RequestParam(defaultValue = "25") int profitMarginPercent) {

        log.info("Calculating sell price: buyPrice={}, margin={}%", buyPrice, profitMarginPercent);

        BigDecimal profitMargin = BigDecimal.valueOf(profitMarginPercent).divide(BigDecimal.valueOf(100));
        BigDecimal sellPrice = buyPrice.multiply(BigDecimal.ONE.add(profitMargin))
                .setScale(2, RoundingMode.HALF_UP);

        Map<String, Object> response = new HashMap<>();
        response.put("buyPrice", buyPrice);
        response.put("profitMarginPercent", profitMarginPercent);
        response.put("sellPrice", sellPrice);
        response.put("profitAmount", sellPrice.subtract(buyPrice));

        return ResponseEntity.ok(ApiResponse.success(response, "Sell price calculated successfully"));
    }
}