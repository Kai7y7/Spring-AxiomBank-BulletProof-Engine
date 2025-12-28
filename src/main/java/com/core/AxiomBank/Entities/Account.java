package com.core.AxiomBank.Entities;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String iban;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal balance;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @JsonIgnore
    private Client owner;


    @Enumerated(EnumType.STRING)
    private AccountStatus status;


    @OneToMany(mappedBy = "account")
    private List<LedgerEntry> ledgerEntries = new ArrayList<>();

    @OneToMany(mappedBy = "fromAccount")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Transaction> outgoingTransactions = new ArrayList<>();


    @OneToMany(mappedBy = "toAccount")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Transaction> incomingTransactions = new ArrayList<>();

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}