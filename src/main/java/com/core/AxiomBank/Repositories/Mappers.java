package com.core.AxiomBank.Repositories;

import com.core.AxiomBank.Dtos.AccountResp;
import com.core.AxiomBank.Dtos.ClientResp;
import com.core.AxiomBank.Dtos.TransactionResp;
import com.core.AxiomBank.Entities.Account;
import com.core.AxiomBank.Entities.Client;
import com.core.AxiomBank.Entities.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface Mappers {

    ClientResp toClientDto(Client client);


    @Mapping(source = "transactionType", target = "type")
    @Mapping(source = "fromAccount.iban", target = "fromAccountIban")
    @Mapping(source = "toAccount.iban", target = "toAccountIban")
    @Mapping(source = "createdAt", target = "timestamp")
    TransactionResp toTransactionDto(Transaction transaction);
    AccountResp toAccountDto(Account account);
}

/*
    private TransactionType transactionType;

    private BigDecimal amount;

    private String transactionReferenceNumber;

    private TransactionDirection transactionDirection;

    private LocalDateTime transactionDate;

 */