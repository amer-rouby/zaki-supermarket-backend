package com.zakisupermarket.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.zakisupermarket.config.jackson.LocalDateDeserializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {
    private Long storeId;

    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate startDate;

    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate endDate;

    private String reportType;
    private String category;
}
