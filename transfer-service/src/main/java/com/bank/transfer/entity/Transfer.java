package com.bank.transfer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfers", schema = "transfer")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transfer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long fromBankAccountId;

    @Column(nullable = false)
    private Long toBankAccountId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String fromCurrency;

    @Column(nullable = false)
    private String toCurrency;

    private BigDecimal convertedAmount;

    @Column(nullable = false)
    private String status;

    private String description;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
