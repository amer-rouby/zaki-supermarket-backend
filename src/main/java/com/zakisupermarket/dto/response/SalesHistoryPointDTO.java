package com.zakisupermarket.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesHistoryPointDTO {
    private LocalDate date;
    private Integer quantity;
    private BigDecimal sales;
}
