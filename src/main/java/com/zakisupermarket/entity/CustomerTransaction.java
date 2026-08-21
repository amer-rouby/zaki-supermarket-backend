package com.zakisupermarket.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Append-only ledger row. Rows are never updated or deleted - a customer's
// currentBalance is always verifiable by summing these (CREDIT_SALE positive,
// PAYMENT negative) and should equal the cached Customer.currentBalance.
@Entity
@Table(name = "customer_transactions", schema = "zaki_supermarket", indexes = {
        @Index(name = "idx_customer_txn_customer", columnList = "customer_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerTransaction {

    public enum Type {
        CREDIT_SALE, PAYMENT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    // CREDIT_SALE is a positive amount (increases what's owed); PAYMENT is
    // stored as a positive amount too, with `type` distinguishing direction -
    // balanceAfter is what actually reflects the running total.
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "related_sale_id")
    private Long relatedSaleId;

    @Column(name = "balance_after", nullable = false, precision = 10, scale = 2)
    private BigDecimal balanceAfter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(length = 255)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
