package com.zakisupermarket.service;

import com.zakisupermarket.dto.request.CategoryRequest;
import com.zakisupermarket.dto.response.CategoryResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CategoryService {

    List<CategoryResponse> getAllCategories(Long storeId);

    Page<CategoryResponse> getCategoriesPage(Long storeId, int page, int size, String search);

    CategoryResponse getCategory(Long id, Long storeId);

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse updateCategory(Long id, CategoryRequest request, Long storeId);

    void deleteCategory(Long id, Long storeId);

    List<CategoryResponse> searchCategories(Long storeId, String query);

    Long getCategoriesCount(Long storeId);

    List<CategoryResponse> getActiveCategories(Long storeId);
}