package com.zakisupermarket.service.impl;

import com.zakisupermarket.dto.request.SupplierRequest;
import com.zakisupermarket.dto.response.SupplierResponse;
import com.zakisupermarket.entity.Store;
import com.zakisupermarket.entity.Supplier;
import com.zakisupermarket.entity.User;
import com.zakisupermarket.repository.StoreRepository;
import com.zakisupermarket.repository.SupplierRepository;
import com.zakisupermarket.service.SupplierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final StoreRepository storeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SupplierResponse> getAllSuppliers(Long storeId) {
        return supplierRepository.findByStoreId(storeId).stream()
                .map(SupplierResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupplierResponse> getSuppliersPaginated(Long storeId, int page, int size) {
        return supplierRepository.findByStoreId(storeId, PageRequest.of(page, size))
                .map(SupplierResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponse getSupplier(Long id, Long storeId) {
        Supplier supplier = supplierRepository.findByIdAndStoreIdAndDeletedAtIsNull(id, storeId)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));
        return SupplierResponse.fromEntity(supplier);
    }

    @Override
    @Transactional
    public SupplierResponse createSupplier(SupplierRequest request, Long storeId, Long userId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));

        if (supplierRepository.existsByStoreIdAndNameAndDeletedAtIsNull(storeId, request.getName())) {
            throw new RuntimeException("Supplier with this name already exists");
        }

        Supplier supplier = Supplier.builder()
                .store(store)
                .name(request.getName())
                .contactPerson(request.getContactPerson())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .city(request.getCity())
                .status(request.getStatus())
                .notes(request.getNotes())
                .build();

        Supplier saved = supplierRepository.save(supplier);
        log.info("Supplier created: id={}, name={}, store={}", saved.getId(), saved.getName(), storeId);
        return SupplierResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public SupplierResponse updateSupplier(Long id, SupplierRequest request, Long storeId, Long userId) {
        Supplier supplier = supplierRepository.findByIdAndStoreIdAndDeletedAtIsNull(id, storeId)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        supplier.setName(request.getName());
        supplier.setContactPerson(request.getContactPerson());
        supplier.setPhone(request.getPhone());
        supplier.setEmail(request.getEmail());
        supplier.setAddress(request.getAddress());
        supplier.setCity(request.getCity());
        supplier.setStatus(request.getStatus());
        supplier.setNotes(request.getNotes());

        Supplier updated = supplierRepository.save(supplier);
        log.info("Supplier updated: id={}, name={}", updated.getId(), updated.getName());
        return SupplierResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteSupplier(Long id, Long storeId, Long userId) {
        Supplier supplier = supplierRepository.findByIdAndStoreIdAndDeletedAtIsNull(id, storeId)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        supplier.setDeletedAt(java.time.LocalDateTime.now());
        supplierRepository.save(supplier);
        log.info("Supplier deleted (soft): id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countSuppliers(Long storeId) {
        return supplierRepository.countByStoreId(storeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierResponse> searchSuppliers(Long storeId, String query) {
        return supplierRepository.searchByStoreId(storeId, query).stream()
                .map(SupplierResponse::fromEntity)
                .toList();
    }
}