package com.zakisupermarket.service;

import com.zakisupermarket.dto.request.SupplierRequest;
import com.zakisupermarket.dto.response.SupplierResponse;
import org.springframework.data.domain.Page;
import java.util.List;

public interface SupplierService {
    List<SupplierResponse> getAllSuppliers(Long storeId);
    Page<SupplierResponse> getSuppliersPaginated(Long storeId, int page, int size);
    SupplierResponse getSupplier(Long id, Long storeId);
    SupplierResponse createSupplier(SupplierRequest request, Long storeId, Long userId);
    SupplierResponse updateSupplier(Long id, SupplierRequest request, Long storeId, Long userId);
    void deleteSupplier(Long id, Long storeId, Long userId);
    Long countSuppliers(Long storeId);
    List<SupplierResponse> searchSuppliers(Long storeId, String query);
}