package com.zakisupermarket.service.impl;

import com.zakisupermarket.dto.request.CustomerPaymentRequest;
import com.zakisupermarket.dto.request.CustomerRequest;
import com.zakisupermarket.dto.response.CustomerResponse;
import com.zakisupermarket.dto.response.CustomerStatementResponse;
import com.zakisupermarket.dto.response.CustomerTransactionResponse;
import com.zakisupermarket.entity.Customer;
import com.zakisupermarket.entity.CustomerTransaction;
import com.zakisupermarket.entity.Store;
import com.zakisupermarket.entity.User;
import com.zakisupermarket.repository.CustomerRepository;
import com.zakisupermarket.repository.CustomerTransactionRepository;
import com.zakisupermarket.repository.StoreRepository;
import com.zakisupermarket.repository.UserRepository;
import com.zakisupermarket.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerTransactionRepository customerTransactionRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers(Long storeId) {
        return customerRepository.findByStoreId(storeId).stream()
                .map(CustomerResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponse> getCustomersPaginated(Long storeId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return customerRepository.findByStoreId(storeId, pageable).map(CustomerResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(Long id, Long storeId) {
        Customer customer = findCustomerOrThrow(id, storeId);
        return CustomerResponse.fromEntity(customer);
    }

    @Override
    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request, Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found: " + storeId));

        if (request.getPhone() != null && !request.getPhone().isBlank()
                && customerRepository.existsByStoreIdAndPhoneAndDeletedAtIsNull(storeId, request.getPhone())) {
            throw new RuntimeException("A customer with this phone number already exists");
        }

        Customer customer = Customer.builder()
                .store(store)
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .creditLimit(request.getCreditLimit() != null ? request.getCreditLimit() : BigDecimal.ZERO)
                .currentBalance(BigDecimal.ZERO)
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .notes(request.getNotes())
                .build();

        Customer saved = customerRepository.save(customer);
        log.info("Customer created: id={}, name={}, storeId={}", saved.getId(), saved.getName(), storeId);
        return CustomerResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomer(Long id, CustomerRequest request, Long storeId) {
        Customer customer = findCustomerOrThrow(id, storeId);

        customer.setName(request.getName());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());
        if (request.getCreditLimit() != null) {
            customer.setCreditLimit(request.getCreditLimit());
        }
        if (request.getStatus() != null) {
            customer.setStatus(request.getStatus());
        }
        customer.setNotes(request.getNotes());

        Customer updated = customerRepository.save(customer);
        log.info("Customer updated: id={}", updated.getId());
        return CustomerResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteCustomer(Long id, Long storeId) {
        Customer customer = findCustomerOrThrow(id, storeId);
        if (customer.getCurrentBalance() != null && customer.getCurrentBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException("Cannot delete a customer with an outstanding balance");
        }
        customer.setDeletedAt(java.time.LocalDateTime.now());
        customerRepository.save(customer);
        log.info("Customer soft-deleted: id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponse> searchCustomers(Long storeId, String query) {
        return customerRepository.searchByStoreId(storeId, query).stream()
                .map(CustomerResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerStatementResponse getStatement(Long id, Long storeId) {
        Customer customer = findCustomerOrThrow(id, storeId);
        List<CustomerTransactionResponse> transactions = customerTransactionRepository
                .findByCustomerIdAndStoreIdOrderByCreatedAtDesc(id, storeId).stream()
                .map(CustomerTransactionResponse::fromEntity)
                .collect(Collectors.toList());

        return CustomerStatementResponse.builder()
                .customer(CustomerResponse.fromEntity(customer))
                .transactions(transactions)
                .build();
    }

    @Override
    @Transactional
    public CustomerResponse recordPayment(Long id, CustomerPaymentRequest request, Long storeId, Long userId) {
        Customer customer = findCustomerOrThrow(id, storeId);
        BigDecimal currentBalance = customer.getCurrentBalance() != null ? customer.getCurrentBalance() : BigDecimal.ZERO;

        if (request.getAmount().compareTo(currentBalance) > 0) {
            throw new RuntimeException("Payment amount exceeds the customer's outstanding balance");
        }

        BigDecimal newBalance = currentBalance.subtract(request.getAmount());
        customer.setCurrentBalance(newBalance);
        customerRepository.save(customer);

        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
        CustomerTransaction ledgerRow = CustomerTransaction.builder()
                .customer(customer)
                .type(CustomerTransaction.Type.PAYMENT)
                .amount(request.getAmount())
                .balanceAfter(newBalance)
                .createdBy(user)
                .notes(request.getNotes())
                .build();
        customerTransactionRepository.save(ledgerRow);

        log.info("Payment recorded for customer {}: amount={}, newBalance={}", id, request.getAmount(), newBalance);
        return CustomerResponse.fromEntity(customer);
    }

    @Override
    @Transactional
    public Customer recordCreditSale(Long customerId, Long storeId, BigDecimal amount, Long saleId, Long userId) {
        Customer customer = findCustomerOrThrow(customerId, storeId);
        if (!customer.isActive()) {
            throw new RuntimeException("Customer account is not active");
        }

        BigDecimal currentBalance = customer.getCurrentBalance() != null ? customer.getCurrentBalance() : BigDecimal.ZERO;
        BigDecimal creditLimit = customer.getCreditLimit() != null ? customer.getCreditLimit() : BigDecimal.ZERO;
        BigDecimal newBalance = currentBalance.add(amount);

        if (newBalance.compareTo(creditLimit) > 0) {
            throw new RuntimeException("Credit limit exceeded for customer " + customer.getName()
                    + ": current balance " + currentBalance + " + " + amount + " would exceed limit " + creditLimit);
        }

        customer.setCurrentBalance(newBalance);
        Customer updated = customerRepository.save(customer);

        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
        CustomerTransaction ledgerRow = CustomerTransaction.builder()
                .customer(updated)
                .type(CustomerTransaction.Type.CREDIT_SALE)
                .amount(amount)
                .relatedSaleId(saleId)
                .balanceAfter(newBalance)
                .createdBy(user)
                .build();
        customerTransactionRepository.save(ledgerRow);

        log.info("Credit sale recorded for customer {}: amount={}, newBalance={}, saleId={}",
                customerId, amount, newBalance, saleId);
        return updated;
    }

    private Customer findCustomerOrThrow(Long id, Long storeId) {
        return customerRepository.findByIdAndStoreIdAndDeletedAtIsNull(id, storeId)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + id));
    }
}
