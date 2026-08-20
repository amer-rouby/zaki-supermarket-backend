package com.zakisupermarket.service.impl;

import com.zakisupermarket.dto.request.CategoryRequest;
import com.zakisupermarket.dto.response.CategoryResponse;
import com.zakisupermarket.entity.Category;
import com.zakisupermarket.entity.Store;
import com.zakisupermarket.repository.CategoryRepository;
import com.zakisupermarket.repository.StoreRepository;
import com.zakisupermarket.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories(Long storeId) {
        return categoryRepository.findByStoreId(storeId)
                .stream()
                .map(CategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryResponse> getCategoriesPage(Long storeId, int page, int size, String search) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.ASC, "nameAr"));
        return categoryRepository.searchAndFilter(storeId, search, pageable)
                .map(CategoryResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategory(Long id, Long storeId) {
        Category category = categoryRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        return CategoryResponse.fromEntity(category);
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String categoryName = resolveCategoryName(request);
        String nameAr = resolveNameAr(request, categoryName);
        String nameEn = resolveNameEn(request, categoryName);

        // Check if category with same name exists
        if (categoryRepository.existsByStoreIdAndNameIgnoreCase(request.getStoreId(), categoryName)) {
            throw new RuntimeException("Category with this name already exists");
        }

        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new RuntimeException("Store not found"));

        Category category = Category.builder()
                .name(categoryName)
                .nameAr(nameAr)
                .nameEn(nameEn)
                .description(request.getDescription())
                .icon(request.getIcon())
                .color(request.getColor() != null ? request.getColor() : "#667eea")
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .store(store)
                .build();

        Category saved = categoryRepository.save(category);
        log.info("Category created: {} for store {}", saved.getName(), store.getId());
        return CategoryResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request, Long storeId) {
        Category category = categoryRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        String categoryName = resolveCategoryName(request);
        String nameAr = resolveNameAr(request, categoryName);
        String nameEn = resolveNameEn(request, categoryName);

        // Check if new name conflicts with existing category
        if (!category.getName().equalsIgnoreCase(categoryName) &&
                categoryRepository.existsByStoreIdAndNameIgnoreCase(storeId, categoryName)) {
            throw new RuntimeException("Category with this name already exists");
        }

        category.setName(categoryName);
        category.setNameAr(nameAr);
        category.setNameEn(nameEn);
        category.setDescription(request.getDescription());
        category.setIcon(request.getIcon());
        category.setColor(request.getColor() != null ? request.getColor() : category.getColor());
        category.setIsActive(request.getIsActive() != null ? request.getIsActive() : category.getIsActive());

        Category updated = categoryRepository.save(category);
        log.info("Category updated: {} for store {}", updated.getName(), storeId);
        return CategoryResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id, Long storeId) {
        Category category = categoryRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Soft delete
        category.setDeletedAt(LocalDateTime.now());
        categoryRepository.save(category);
        log.info("Category deleted (soft): {} for store {}", category.getName(), storeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> searchCategories(Long storeId, String query) {
        return categoryRepository.searchByStoreIdAndName(storeId, query)
                .stream()
                .map(CategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Long getCategoriesCount(Long storeId) {
        return categoryRepository.countByStoreId(storeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getActiveCategories(Long storeId) {
        return categoryRepository.findActiveByStoreId(storeId)
                .stream()
                .map(CategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private String resolveCategoryName(CategoryRequest request) {
        String name = cleanText(request.getName());
        if (name != null) {
            return name;
        }

        String nameAr = cleanText(request.getNameAr());
        if (nameAr != null) {
            return nameAr;
        }

        return cleanText(request.getNameEn());
    }

    private String resolveNameAr(CategoryRequest request, String fallbackName) {
        String nameAr = cleanText(request.getNameAr());
        return nameAr != null ? nameAr : fallbackName;
    }

    private String resolveNameEn(CategoryRequest request, String fallbackName) {
        String nameEn = cleanText(request.getNameEn());
        return nameEn != null ? nameEn : fallbackName;
    }

    private String cleanText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
