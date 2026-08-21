package com.zakisupermarket.service;

import com.zakisupermarket.dto.response.EInvoiceSubmissionResponse;

public interface EInvoiceService {
    EInvoiceSubmissionResponse submit(Long saleId, Long storeId);

    EInvoiceSubmissionResponse retry(Long saleId, Long storeId);

    EInvoiceSubmissionResponse getForSale(Long saleId, Long storeId);
}
