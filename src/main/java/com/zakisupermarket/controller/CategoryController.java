package com.zakisupermarket.controller;

import com.zakisupermarket.dto.request.CategoryRequest;
import com.zakisupermarket.dto.response.ApiResponse;
import com.zakisupermarket.dto.response.CategoryResponse;
import com.zakisupermarket.service.CategoryService;
import com.zakisupermarket.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories(
            @RequestParam Long storeId) {
        storeId = SecurityUtils.getCurrentStoreId();

        log.info("GET /api/categories - storeId: {}", storeId);

        List<CategoryResponse> categories = categoryService.getAllCategories(storeId);
        return ResponseEntity.ok(ApiResponse.success(categories, "Categories retrieved successfully"));
    }

    /**
     * Real backend-paginated + searchable category listing for the Categories
     * management screen. Separate from GET /api/categories (above) and GET
     * /api/categories/active, which stay unpaginated since the product form's
     * category dropdown and other consumers depend on getting every category back
     * in one call.
     */
    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'VIEWER')")
    public ResponseEntity<ApiResponse<Page<CategoryResponse>>> getCategoriesPage(
            @RequestParam Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        storeId = SecurityUtils.getCurrentStoreId();

        log.info("GET /api/categories/page - storeId: {}, page: {}, size: {}, search: '{}'",
                storeId, page, size, search);

        Page<CategoryResponse> categories = categoryService.getCategoriesPage(storeId, page, size, search);
        return ResponseEntity.ok(ApiResponse.success(categories, "Categories retrieved successfully"));
    }

    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'VIEWER')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getCategoriesCount(
            @RequestParam Long storeId) {
        storeId = SecurityUtils.getCurrentStoreId();

        log.info("GET /api/categories/count - storeId: {}", storeId);

        Long count = categoryService.getCategoriesCount(storeId);
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);

        return ResponseEntity.ok(ApiResponse.success(response, "Categories count retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'VIEWER')")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategory(
            @PathVariable Long id,
            @RequestParam Long storeId) {
        storeId = SecurityUtils.getCurrentStoreId();

        log.info("GET /api/categories/{} - storeId: {}", id, storeId);

        CategoryResponse category = categoryService.getCategory(id, storeId);
        return ResponseEntity.ok(ApiResponse.success(category, "Category retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CategoryRequest request) {
        request.setStoreId(SecurityUtils.getCurrentStoreId());

        log.info("POST /api/categories - storeId: {}, categoryName: {}",
                request.getStoreId(), request.getName());

        CategoryResponse category = categoryService.createCategory(request);
        return ResponseEntity.ok(ApiResponse.success(category, "Category created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request,
            @RequestParam Long storeId) {
        storeId = SecurityUtils.getCurrentStoreId();
        request.setStoreId(storeId);

        log.info("PUT /api/categories/{} - storeId: {}", id, storeId);

        CategoryResponse category = categoryService.updateCategory(id, request, storeId);
        return ResponseEntity.ok(ApiResponse.success(category, "Category updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable Long id,
            @RequestParam Long storeId) {
        storeId = SecurityUtils.getCurrentStoreId();

        log.info("DELETE /api/categories/{} - storeId: {}", id, storeId);

        categoryService.deleteCategory(id, storeId);
        return ResponseEntity.ok(ApiResponse.success(null, "Category deleted successfully"));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> searchCategories(
            @RequestParam Long storeId,
            @RequestParam String query) {
        storeId = SecurityUtils.getCurrentStoreId();

        log.info("GET /api/categories/search - storeId: {}, query: '{}'", storeId, query);

        List<CategoryResponse> categories = categoryService.searchCategories(storeId, query);
        return ResponseEntity.ok(ApiResponse.success(categories, "Search completed successfully"));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getActiveCategories(
            @RequestParam Long storeId) {
        storeId = SecurityUtils.getCurrentStoreId();

        log.info("GET /api/categories/active - storeId: {}", storeId);

        List<CategoryResponse> categories = categoryService.getActiveCategories(storeId);
        return ResponseEntity.ok(ApiResponse.success(categories, "Active categories retrieved successfully"));
    }
}