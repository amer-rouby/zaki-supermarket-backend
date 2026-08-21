package com.zakisupermarket.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerStatementResponse {
    private CustomerResponse customer;
    private List<CustomerTransactionResponse> transactions;
}
