package com.zakisupermarket.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private Long id;
    private Long storeId;
    private String name;
    private String barcode;
    private String category;
    private String unitType;
    private Integer minStockLevel;
    private BigDecimal sellPrice;
    private BigDecimal buyPrice;
    private Map<String, Object> extraAttributes;
    private Integer totalStock;
    private LocalDateTime createdAt;
}