package com.zakisupermarket.service;

import com.zakisupermarket.dto.request.ProductRequest;
import com.zakisupermarket.dto.response.ProductResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ProductService {

    List<ProductResponse> getAllProducts(Long storeId);

    Page<ProductResponse> getProductsPage(Long storeId, int page, int size, String search,
                                           String category, String sortBy, String sortDirection);

    Long getProductsCount(Long storeId);

    ProductResponse getProduct(Long id, Long storeId);

    ProductResponse createProduct(ProductRequest request, Long storeId);

    ProductResponse updateProduct(Long id, ProductRequest request, Long storeId);

    void deleteProduct(Long id, Long storeId);

    List<ProductResponse> searchProducts(Long storeId, String query);

    List<ProductResponse> getLowStockProducts(Long storeId);
}