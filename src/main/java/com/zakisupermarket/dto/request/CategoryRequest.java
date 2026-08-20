package com.zakisupermarket.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    private String name;

    private String nameAr;

    private String nameEn;

    private String description;

    private String icon;

    @Builder.Default
    private String color = "#667eea";

    @NotNull(message = "Store is required")
    private Long storeId;

    @Builder.Default
    private Boolean isActive = true;
}
