package com.zakisupermarket.service.impl;

import com.zakisupermarket.dto.request.ProductRequest;
import com.zakisupermarket.dto.response.ProductResponse;
import com.zakisupermarket.entity.Store;
import com.zakisupermarket.entity.Product;
import com.zakisupermarket.entity.StockBatch;
import com.zakisupermarket.repository.StoreRepository;
import com.zakisupermarket.repository.ProductRepository;
import com.zakisupermarket.repository.StockBatchRepository;
import com.zakisupermarket.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final StockBatchRepository stockBatchRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts(Long storeId) {
        return productRepository.findActiveProductsByStore(storeId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsPage(Long storeId, int page, int size, String search,
                                                  String category, String sortBy, String sortDirection) {
        Pageable pageable = createPageable(page, size, sortBy, sortDirection);
        return productRepository.searchAndFilter(storeId, search, category, pageable)
                .map(this::mapToResponse);
    }

    private Pageable createPageable(int page, int size, String sortBy, String sortDirection) {
        List<String> allowedFields = Arrays.asList("name", "sellPrice", "category", "createdAt");
        String safeSortBy = allowedFields.contains(sortBy) ? sortBy : "name";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(direction, safeSortBy));
    }

    @Override
    @Transactional(readOnly = true)
    public Long getProductsCount(Long storeId) {
        return productRepository.countByStoreId(storeId);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id, Long storeId) {
        Product product = productRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return mapToResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request, Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));

        if (request.getBarcode() != null && !request.getBarcode().trim().isEmpty()) {
            if (productRepository.findByStoreIdAndBarcode(storeId, request.getBarcode().trim()).isPresent()) {
                throw new RuntimeException("Barcode already exists");
            }
        }

        Product product = Product.builder()
                .store(store)
                .name(request.getName())
                .barcode(request.getBarcode())
                .category(request.getCategory())
                .unitType(request.getUnitType())
                .minStockLevel(request.getMinStockLevel())
                .sellPrice(request.getSellPrice())
                .buyPrice(request.getBuyPrice())
                .extraAttributes(request.getExtraAttributes())
                .build();

        productRepository.save(product);
        log.info("Product created: id={}, name={}", product.getId(), product.getName());
        if (request.getInitialStock() != null && request.getInitialStock() > 0) {
            BigDecimal effectiveBuyPrice = request.getBuyPrice() != null
                    ? request.getBuyPrice()
                    : request.getSellPrice();

            LocalDate expiryDate = request.getExpiryDate() != null
                    ? request.getExpiryDate()
                    : LocalDate.now().plusMonths(24);

            StockBatch batch = StockBatch.builder()
                    .product(product)
                    .store(store)
                    .batchNumber("BATCH-" + product.getId() + "-" + System.currentTimeMillis())
                    .quantityInitial(request.getInitialStock())
                    .quantityCurrent(request.getInitialStock())
                    .expiryDate(expiryDate)
                    .buyPrice(effectiveBuyPrice)
                    .sellPrice(request.getSellPrice())
                    .location("Shelf-1")
                    .status(StockBatch.BatchStatus.ACTIVE)
                    .build();
            stockBatchRepository.save(batch);
            // product.stockBatches is a plain empty ArrayList on this just-built entity
            // (not a Hibernate lazy proxy, since it was never loaded from the DB), so it
            // won't pick up the batch we just saved unless we add it ourselves - without
            // this, getTotalStock() below reports 0 even though the batch exists in the DB.
            product.getStockBatches().add(batch);
            log.info("Initial stock batch created: productId={}, quantity={}, buyPrice={}",
                    product.getId(), request.getInitialStock(), effectiveBuyPrice);
        }

        return mapToResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request, Long storeId) {
        Product product = productRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setName(request.getName());
        product.setBarcode(request.getBarcode());
        product.setCategory(request.getCategory());
        product.setUnitType(request.getUnitType());
        product.setMinStockLevel(request.getMinStockLevel());

        if (request.getBuyPrice() != null) {
            product.setBuyPrice(request.getBuyPrice());

            if (request.getSellPrice() == null) {
                BigDecimal profitMargin = BigDecimal.valueOf(0.25);
                BigDecimal autoSellPrice = request.getBuyPrice()
                        .multiply(BigDecimal.ONE.add(profitMargin))
                        .setScale(2, RoundingMode.HALF_UP);
                product.setSellPrice(autoSellPrice);
                log.info("Auto-calculated sell price: buyPrice={}, sellPrice={}, margin=25%",
                        request.getBuyPrice(), autoSellPrice);
            } else {
                product.setSellPrice(request.getSellPrice());
            }
        } else if (request.getSellPrice() != null) {
            product.setSellPrice(request.getSellPrice());
        }

        product.setExtraAttributes(request.getExtraAttributes());

        Product updated = productRepository.save(product);
        log.info("Product updated: id={}, name={}, buyPrice={}, sellPrice={}",
                updated.getId(), updated.getName(), updated.getBuyPrice(), updated.getSellPrice());

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id, Long storeId) {
        Product product = productRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (product.getDeletedAt() != null) {
            log.warn("Product already deleted: id={}", id);
            return;
        }

        product.setDeletedAt(LocalDateTime.now());
        productRepository.save(product);
        log.info("Product soft deleted: id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> searchProducts(Long storeId, String query) {
        return productRepository.findByStoreIdAndNameContainingIgnoreCase(storeId, query)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getLowStockProducts(Long storeId) {
        return productRepository.findLowStockProducts(storeId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ProductResponse mapToResponse(Product product) {
        if (product == null) return null;

        return ProductResponse.builder()
                .id(product.getId())
                .storeId(product.getStore() != null ? product.getStore().getId() : null)
                .name(product.getName())
                .barcode(product.getBarcode())
                .category(product.getCategory())
                .unitType(product.getUnitType())
                .minStockLevel(product.getMinStockLevel())
                .sellPrice(product.getSellPrice())
                .buyPrice(product.getBuyPrice())
                .extraAttributes(product.getExtraAttributes())
                .totalStock(product.getTotalStock())
                .createdAt(product.getCreatedAt())
                .build();
    }
}