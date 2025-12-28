package com.core.AxiomBank.Dtos;


import com.core.AxiomBank.Entities.Account;
import com.core.AxiomBank.Entities.TransactionStatus;
import com.core.AxiomBank.Entities.TransactionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionResp {
    private String referenceNumber;
    private BigDecimal amount;
    private String status;
    private String type;
    private String fromAccountIban;
    private String toAccountIban;
    private LocalDateTime timestamp;
}