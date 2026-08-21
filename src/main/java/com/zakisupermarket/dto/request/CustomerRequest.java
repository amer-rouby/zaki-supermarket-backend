package com.zakisupermarket.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CustomerRequest {

    @NotBlank(message = "Customer name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phone;

    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    private BigDecimal creditLimit;

    @Size(max = 20, message = "Invalid customer status")
    @Builder.Default
    private String status = "ACTIVE";

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
