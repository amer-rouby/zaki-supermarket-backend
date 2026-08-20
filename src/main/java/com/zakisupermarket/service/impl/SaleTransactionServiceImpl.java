package com.zakisupermarket.service.impl;

import com.zakisupermarket.dto.request.SaleItemRequest;
import com.zakisupermarket.dto.request.SaleRequest;
import com.zakisupermarket.dto.response.SaleTransactionDTO;
import com.zakisupermarket.dto.response.SalesReportResponse;
import com.zakisupermarket.entity.*;
import com.zakisupermarket.entity.enums.PaymentMethod;
import com.zakisupermarket.repository.*;
import com.zakisupermarket.repository.settings.StoreSettingsRepository;
import com.zakisupermarket.service.NotificationService;
import com.zakisupermarket.service.SaleTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaleTransactionServiceImpl implements SaleTransactionService {

    private final SaleTransactionRepository saleTransactionRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final StockBatchRepository stockBatchRepository;
    private final SaleItemRepository saleItemRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final StoreSettingsRepository storeSettingsRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<SaleTransactionDTO> getAllSales(Long storeId, int page, int size) {
        return getAllSales(storeId, page, size, "transactionDate", "desc");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SaleTransactionDTO> getAllSales(Long storeId, int page, int size,
                                                String sortBy, String sortDirection) {
        log.debug("Fetching sales | storeId: {} | page: {} | size: {} | sortBy: {} | sortDirection: {}",
                storeId, page, size, sortBy, sortDirection);
        Pageable pageable = createPageable(sortBy, sortDirection, page, size);
        return saleTransactionRepository.findByStoreId(storeId, pageable)
                .map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public SaleTransactionDTO getSaleById(Long id, Long storeId) {
        log.debug("Fetching sale | id: {} | storeId: {}", id, storeId);
        SaleTransaction sale = saleTransactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sale not found with id: " + id));
        validateStoreAccess(sale, storeId);
        return mapToDTO(sale);
    }

    @Override
    @Transactional
    public SaleTransactionDTO createSale(SaleRequest request, Long currentUserId) {
        log.info("Creating new sale | storeId: {} | items: {} | userId: {}",
                request.getStoreId(), request.getItems().size(), currentUserId);

        if (currentUserId == null) {
            throw new RuntimeException("Cannot create sale: no authenticated user could be resolved");
        }

        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new RuntimeException("Store not found: " + request.getStoreId()));
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found: " + currentUserId));

        String paymentMethodValue = Optional.ofNullable(request.getPaymentMethod()).orElse("CASH").toUpperCase();
        validatePaymentMethodEnabled(request.getStoreId(), paymentMethodValue);

        SaleTransaction sale = SaleTransaction.builder()
                .store(store)
                .user(user)
                .invoiceNumber(generateInvoiceNumber())
                .discountAmount(Optional.ofNullable(request.getDiscountAmount()).orElse(BigDecimal.ZERO))
                .customerPhone(request.getCustomerPhone())
                .paymentMethod(PaymentMethod.valueOf(paymentMethodValue))
                .notes(request.getNotes())
                .build();

        List<SaleItem> saleItems = new ArrayList<>();
        for (SaleItemRequest itemRequest : request.getItems()) {
            SaleItem saleItem = processSaleItem(itemRequest, sale);
            saleItems.add(saleItem);
        }

        sale.setItems(saleItems);
        sale.calculateTotals();

        SaleTransaction savedSale = saleTransactionRepository.save(sale);
        log.info("Sale created successfully | id: {} | total: {} | items: {}",
                savedSale.getId(), savedSale.getTotalAmount(), savedSale.getItems().size());
        try {
            notificationService.notifySaleCompleted(store.getId(), savedSale.getId(), savedSale.getTotalAmount());
        } catch (Exception e) {
            log.warn("Failed to create sale notification for sale {}: {}", savedSale.getId(), e.getMessage());
        }
        return mapToDTO(savedSale);
    }

    /** The POS UI already hides disabled payment-method buttons, but that's cosmetic
     * only - without this check, any payment method could still be submitted directly
     * via the API regardless of what the store has disabled in settings. */
    private void validatePaymentMethodEnabled(Long storeId, String paymentMethodValue) {
        String enabled = storeSettingsRepository.findByStoreId(storeId)
                .map(s -> s.getEnabledPaymentMethods())
                .orElse(null);
        if (enabled == null || enabled.isBlank()) {
            return;
        }
        List<String> enabledMethods = Arrays.stream(enabled.split(","))
                .map(String::trim).map(String::toUpperCase).filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        if (!enabledMethods.contains(paymentMethodValue)) {
            throw new RuntimeException("Payment method '" + paymentMethodValue + "' is not enabled for this store");
        }
    }

    @Override
    @Transactional
    public SaleTransactionDTO updateSale(Long id, SaleRequest request, Long storeId) {
        log.info("Updating sale | id: {} | storeId: {}", id, storeId);
        SaleTransaction entity = saleTransactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sale not found: " + id));
        validateStoreAccess(entity, storeId);

        Optional.ofNullable(request.getDiscountAmount()).ifPresent(entity::setDiscountAmount);
        Optional.ofNullable(request.getPaymentMethod())
                .ifPresent(pm -> entity.setPaymentMethod(PaymentMethod.valueOf(pm.toUpperCase())));
        Optional.ofNullable(request.getCustomerPhone()).ifPresent(entity::setCustomerPhone);
        Optional.ofNullable(request.getNotes()).ifPresent(entity::setNotes);

        entity.calculateTotals();
        SaleTransaction updated = saleTransactionRepository.save(entity);
        log.info("Sale updated successfully | id: {} | total: {}", updated.getId(), updated.getTotalAmount());
        return mapToDTO(updated);
    }

    @Override
    @Transactional
    public void deleteSale(Long id, Long storeId) {
        log.info("Soft deleting sale | id: {} | storeId: {}", id, storeId);
        SaleTransaction sale = saleTransactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sale not found: " + id));
        validateStoreAccess(sale, storeId);
        sale.markAsDeleted();
        saleTransactionRepository.save(sale);
        log.info("Sale deleted successfully | id: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public SalesReportResponse getSalesAnalytics(Long storeId, LocalDate startDate,
                                                 LocalDate endDate, String period) {
        log.info("Generating sales analytics | storeId: {} | period: {} to {}",
                storeId, startDate, endDate);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        BigDecimal totalRevenue = Optional.ofNullable(
                saleTransactionRepository.getTotalRevenue(storeId, startDateTime, endDateTime)
        ).orElse(BigDecimal.ZERO);

        Long totalOrders = Optional.ofNullable(
                saleTransactionRepository.getTotalOrders(storeId, startDateTime, endDateTime)
        ).orElse(0L);

        BigDecimal averageOrder = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<SalesReportResponse.TopProductDTO> topProducts = fetchTopProducts(
                storeId, startDateTime, endDateTime, 10);

        Map<String, BigDecimal> revenueByPaymentMethod = fetchRevenueByPaymentMethod(
                storeId, startDateTime, endDateTime);

        Map<String, Long> ordersByDay = fetchDailyOrders(storeId, startDateTime, endDateTime);

        Long totalItems = saleItemRepository.sumQuantityByStoreIdAndDateRange(
                storeId, startDateTime, endDateTime);

        return SalesReportResponse.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .averageOrder(averageOrder)
                .totalItems(Optional.ofNullable(totalItems).orElse(0L))
                .revenueByPaymentMethod(revenueByPaymentMethod)
                .ordersByDay(ordersByDay)
                .topProducts(topProducts)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSalesStats(Long storeId) {
        log.debug("Fetching sales stats for storeId: {}", storeId);
        LocalDate today = LocalDate.now();
        LocalDateTime startDate = today.atStartOfDay();
        LocalDateTime endDate = today.atTime(LocalTime.MAX);

        Long todayCount = Optional.ofNullable(
                saleTransactionRepository.countByStoreIdAndDateRange(storeId, startDate, endDate)
        ).orElse(0L);

        BigDecimal todayTotal = Optional.ofNullable(
                saleTransactionRepository.sumTotalAmountByStoreIdAndDateRange(storeId, startDate, endDate)
        ).orElse(BigDecimal.ZERO);

        Long totalProducts = productRepository.countByStoreId(storeId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("todaySales", todayTotal);
        stats.put("todayCount", todayCount);
        stats.put("totalProducts", totalProducts);
        stats.put("timestamp", LocalDateTime.now());
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getTodaySales(Long storeId) {
        log.debug("Fetching today sales for storeId: {}", storeId);
        return getSalesForDate(storeId, LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SaleTransactionDTO> getSalesByDateRange(Long storeId, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching sales by date range | storeId: {} | {} to {}",
                storeId, startDate, endDate);
        return saleTransactionRepository.findByStoreIdAndDateRange(
                storeId,
                startDate.atStartOfDay(),
                endDate.atTime(LocalTime.MAX),
                PageRequest.of(0, 20)
        ).map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SaleTransactionDTO> searchSales(Long storeId, String query) {
        log.info("Searching sales | storeId: {} | query: {}", storeId, query);
        if (query == null || query.trim().isEmpty()) {
            return Page.empty();
        }
        return saleTransactionRepository.searchSales(storeId, query.trim(), PageRequest.of(0, 20))
                .map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SaleTransactionDTO> getRecentSales(Long storeId, int limit) {
        log.debug("Fetching recent sales | storeId: {} | limit: {}", storeId, limit);
        int safeLimit = Math.max(1, Math.min(limit, 50));
        return saleTransactionRepository.findRecentSalesByStoreId(storeId, PageRequest.of(0, safeLimit))
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSalesByCategory(Long storeId, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching sales by category | storeId: {} | {} to {}",
                storeId, startDate, endDate);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<SaleItem> saleItems = saleItemRepository.findByStoreIdAndDateRange(
                storeId, startDateTime, endDateTime);
        Map<String, BigDecimal> salesByCategory = saleItems.stream()
                .filter(item -> {
                    try {
                        Product product = item.getProduct();
                        if (product == null) return false;
                        if (!Hibernate.isInitialized(product)) {
                            Hibernate.initialize(product);
                        }
                        if (product.getDeletedAt() != null) return false;
                        return product.getCategory() != null;
                    } catch (Exception e) {
                        log.warn("Skipping sale item {} due to product error: {}",
                                item.getId(), e.getMessage());
                        return false;
                    }
                })
                .collect(Collectors.groupingBy(
                        item -> {
                            try {
                                return item.getProduct().getCategory();
                            } catch (Exception e) {
                                return "Other";
                            }
                        },
                        Collectors.reducing(BigDecimal.ZERO, SaleItem::getTotalPrice, BigDecimal::add)
                ));

        Map<String, Object> response = new HashMap<>();
        response.put("salesByCategory", salesByCategory);
        response.put("totalCategories", salesByCategory.size());
        response.put("dateRange", Map.of("start", startDate, "end", endDate));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopProducts(Long storeId, int limit) {
        log.info("Fetching top products | storeId: {} | limit: {}", storeId, limit);
        int safeLimit = Math.max(1, Math.min(limit, 20));
        Pageable pageable = PageRequest.of(0, safeLimit);
        List<Object[]> topProducts = saleItemRepository.findTopSellingProducts(storeId, pageable);

        return topProducts.stream()
                .map(obj -> {
                    Map<String, Object> productMap = new LinkedHashMap<>();
                    productMap.put("productId", obj[0]);
                    productMap.put("productName", obj[1]);
                    productMap.put("totalQuantity", convertToLong(obj[2]));
                    productMap.put("totalRevenue", obj[3]);
                    return productMap;
                })
                .collect(Collectors.toList());
    }

    private Pageable createPageable(String sortBy, String sortDirection, int page, int size) {
        Sort sort = createSort(sortBy, sortDirection);
        return PageRequest.of(page, size, sort);
    }

    private Sort createSort(String sortBy, String sortDirection) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        List<String> allowedFields = Arrays.asList("transactionDate", "totalAmount", "invoiceNumber", "id");
        if (!allowedFields.contains(sortBy)) {
            sortBy = "transactionDate";
        }
        return Sort.by(direction, sortBy);
    }

    private SaleItem processSaleItem(SaleItemRequest itemRequest, SaleTransaction sale) {
        Product product = productRepository.findById(itemRequest.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found: " + itemRequest.getProductId()));

        StockBatch selectedBatch = selectStockBatch(product.getId(), itemRequest.getQuantity());
        if (selectedBatch == null) {
            throw new RuntimeException("Insufficient stock for product: " + product.getName());
        }

        SaleItem saleItem = SaleItem.builder()
                .product(product)
                .batch(selectedBatch)
                .quantity(itemRequest.getQuantity())
                .unitPrice(itemRequest.getUnitPrice())
                .transaction(sale)
                .build();

        saleItem.calculateTotalPrice();
        deductStock(selectedBatch, itemRequest.getQuantity());
        return saleItem;
    }

    private StockBatch selectStockBatch(Long productId, Integer requestedQuantity) {
        List<StockBatch> activeBatches = stockBatchRepository.findByProductIdAndStatusActive(productId);
        if (activeBatches == null || activeBatches.isEmpty()) {
            return null;
        }
        activeBatches.sort(Comparator.comparing(StockBatch::getExpiryDate));

        Integer remainingQuantity = requestedQuantity;
        StockBatch selectedBatch = null;

        for (StockBatch batch : activeBatches) {
            Integer available = Optional.ofNullable(batch.getQuantityCurrent()).orElse(0);
            if (available >= remainingQuantity) {
                selectedBatch = batch;
                break;
            } else if (available > 0) {
                remainingQuantity -= available;
                if (selectedBatch == null) {
                    selectedBatch = batch;
                }
            }
        }
        return selectedBatch;
    }

    private void deductStock(StockBatch batch, Integer quantity) {
        if (batch == null || batch.getQuantityCurrent() == null) {
            return;
        }
        Integer newQuantity = batch.getQuantityCurrent() - quantity;
        if (newQuantity < 0) {
            throw new RuntimeException("Stock deduction error: insufficient quantity");
        }
        batch.setQuantityCurrent(newQuantity);
        stockBatchRepository.save(batch);
        log.debug("Stock deducted | batch: {} | quantity: {} | remaining: {}",
                batch.getId(), quantity, newQuantity);
    }

    private String generateInvoiceNumber() {
        return "INV-" + System.currentTimeMillis() + "-" +
                String.format("%04d", new Random().nextInt(10000));
    }

    private void validateStoreAccess(SaleTransaction sale, Long storeId) {
        if (!sale.getStore().getId().equals(storeId)) {
            log.warn("Access denied | sale: {} | requested store: {} | actual store: {}",
                    sale.getId(), storeId, sale.getStore().getId());
            throw new RuntimeException("Access denied: Sale does not belong to this store");
        }
    }

    private Map<String, Object> getSalesForDate(Long storeId, LocalDate date) {
        LocalDateTime startDate = date.atStartOfDay();
        LocalDateTime endDate = date.atTime(LocalTime.MAX);

        Long count = Optional.ofNullable(
                saleTransactionRepository.countByStoreIdAndDateRange(storeId, startDate, endDate)
        ).orElse(0L);

        BigDecimal total = Optional.ofNullable(
                saleTransactionRepository.sumTotalAmountByStoreIdAndDateRange(storeId, startDate, endDate)
        ).orElse(BigDecimal.ZERO);

        Map<String, Object> response = new HashMap<>();
        response.put("totalAmount", total);
        response.put("count", count);
        response.put("date", date);
        return response;
    }

    private List<SalesReportResponse.TopProductDTO> fetchTopProducts(Long storeId,
                                                                     LocalDateTime start,
                                                                     LocalDateTime end,
                                                                     int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Object[]> data = saleTransactionRepository.getTopProducts(storeId, start, end, pageable);

        return data.stream()
                .map(obj -> SalesReportResponse.TopProductDTO.builder()
                        .productId((Long) obj[0])
                        .productName((String) obj[1])
                        .quantitySold(convertToLong(obj[2]))
                        .totalRevenue((BigDecimal) obj[3])
                        .build())
                .collect(Collectors.toList());
    }

    private Map<String, BigDecimal> fetchRevenueByPaymentMethod(Long storeId,
                                                                LocalDateTime start,
                                                                LocalDateTime end) {
        List<Object[]> data = saleTransactionRepository.getRevenueByPaymentMethod(storeId, start, end);
        return data.stream()
                .collect(Collectors.toMap(
                        obj -> ((PaymentMethod) obj[0]).name(),
                        obj -> (BigDecimal) obj[1],
                        BigDecimal::add,
                        LinkedHashMap::new
                ));
    }

    private Map<String, Long> fetchDailyOrders(Long storeId, LocalDateTime start, LocalDateTime end) {
        List<Object[]> data = saleTransactionRepository.getDailySales(storeId, start, end);
        return data.stream()
                .collect(Collectors.toMap(
                        obj -> {
                            if (obj[0] instanceof java.sql.Date) {
                                return ((java.sql.Date) obj[0]).toLocalDate().toString();
                            }
                            return obj[0].toString();
                        },
                        obj -> (Long) obj[2],
                        Long::sum,
                        LinkedHashMap::new
                ));
    }

    private Long convertToLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof Number) return ((Number) value).longValue();
        return 0L;
    }

    private SaleTransactionDTO mapToDTO(SaleTransaction sale) {
        if (sale == null) return null;
        return SaleTransactionDTO.builder()
                .id(sale.getId())
                .invoiceNumber(sale.getInvoiceNumber())
                .subtotal(sale.getSubtotal())
                .totalAmount(sale.getTotalAmount())
                .discountAmount(sale.getDiscountAmount())
                .paymentMethod(sale.getPaymentMethod() != null ? sale.getPaymentMethod().name() : null)
                .customerPhone(sale.getCustomerPhone())
                .notes(sale.getNotes())
                .transactionDate(sale.getTransactionDate())
                .storeId(sale.getStore().getId())
                .storeName(sale.getStore().getName())
                .items(Optional.ofNullable(sale.getItems()).orElse(Collections.emptyList()).stream()
                        .map(this::mapItemToDTO)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .build();
    }

    private SaleTransactionDTO.SaleItemDTO mapItemToDTO(SaleItem item) {
        try {
            if (item == null) return null;

            String productName = "Product unavailable";
            String productBarcode = null;
            Long productId = null;
            String productCategory = null;

            try {
                Product product = item.getProduct();
                if (product != null) {
                    if (!Hibernate.isInitialized(product)) {
                        try {
                            Hibernate.initialize(product);
                        } catch (Exception e) {
                            log.warn("Could not initialize product for sale item {}: {}",
                                    item.getId(), e.getMessage());
                        }
                    }

                    if (product.getDeletedAt() == null) {
                        productId = product.getId();
                        productName = product.getName() != null ?
                                product.getName() : "Product unavailable";
                        productBarcode = product.getBarcode();
                        productCategory = product.getCategory();
                    }
                }
            } catch (Exception e) {
                log.warn("Error loading product for sale item {}: {}", item.getId(), e.getMessage());
            }

            return SaleTransactionDTO.SaleItemDTO.builder()
                    .id(item.getId())
                    .productId(productId)
                    .productName(productName)
                    .category(productCategory)
                    .barcode(productBarcode)
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .totalPrice(item.getTotalPrice())
                    .batchNumber(item.getBatch() != null ? item.getBatch().getBatchNumber() : null)
                    .expiryDate(item.getBatch() != null ? item.getBatch().getExpiryDate() : null)
                    .build();

        } catch (Exception e) {
            log.error("Failed to map sale item {} to DTO", item != null ? item.getId() : "unknown", e);
            return SaleTransactionDTO.SaleItemDTO.builder()
                    .id(item != null ? item.getId() : null)
                    .productName("Error loading data")
                    .build();
        }
    }
}