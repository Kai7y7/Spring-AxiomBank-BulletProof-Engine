package com.core.AxiomBank.Services;


import com.core.AxiomBank.Dtos.AccountResp;
import com.core.AxiomBank.Dtos.SetCurrencyReq;
import com.core.AxiomBank.Entities.Account;
import com.core.AxiomBank.Entities.AccountStatus;
import com.core.AxiomBank.Entities.Client;
import com.core.AxiomBank.Entities.ClientStatus;
import com.core.AxiomBank.Exceptions.BadRequestException;
import com.core.AxiomBank.Exceptions.ForbiddenActionException;
import com.core.AxiomBank.Exceptions.NotFoundException;
import com.core.AxiomBank.Repositories.AccountRepository;
import com.core.AxiomBank.Repositories.ClientRepository;
import com.core.AxiomBank.Repositories.Mappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final Mappers mappers;
    private final ClientRepository clientRepository;

    @Transactional
    public AccountResp registerNewAccount(SetCurrencyReq setCurrencyReq, Long clientId) {
        log.info("Request to open new {} account for client ID: {}", setCurrencyReq.getCurrency(), clientId);

        Client client = clientRepository.findById(clientId).orElseThrow(() -> new NotFoundException("Client not found"));


        // Security Check
        if (client.getClientStatus() != ClientStatus.ACTIVE) {
            log.warn("Security Block: Client {} attempted to open account but status is {}", clientId, client.getClientStatus());
            throw new ForbiddenActionException("Client is not active/verified.");
        }

        Account account = new Account();
        account.setCurrency(setCurrencyReq.getCurrency());
        account.setBalance(BigDecimal.ZERO);
        account.setIban(GeneralMethodsService.generateIban(client.getCountry()));
        account.setOwner(client);
        account.setStatus(AccountStatus.ACTIVE);

        Account saved = accountRepository.save(account);
        log.info("New account created successfully. IBAN: {}, Internal ID: {}", saved.getIban(), saved.getId());

        return mappers.toAccountDto(saved);
    }

    @Transactional
    public AccountResp changeAccountCurrency(Long accountId, SetCurrencyReq req, Long clientId) {
        log.info("Request to change currency to {} for account ID: {}", req.getCurrency(), accountId);

        Account targetAccount = getAccount(accountId, clientId);


        if (targetAccount.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            log.warn("Refused currency change for account {}: Balance is not zero ({})",
                    accountId, targetAccount.getBalance());
            throw new BadRequestException("Cannot change currency of an account with a remaining balance.");
        }

        targetAccount.setCurrency(req.getCurrency());
        log.info("Account {} currency updated to {}", accountId, req.getCurrency());

        return mappers.toAccountDto(targetAccount);
    }

    @Transactional
    public void closeAccount(Long accountId, Long clientId) {
        log.info("Closing account ID: {} for client ID: {}", accountId, clientId);

        Account targetAccount = getAccount(accountId, clientId);

        if (targetAccount.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            log.warn("Closure failed: Account {} still has funds", accountId);
            throw new BadRequestException("Withdraw all funds before closing the account.");
        }

        targetAccount.setStatus(AccountStatus.CLOSED);
        log.info("Account {} successfully CLOSED", accountId);
    }


    private Account getAccount(Long accountId, Long clientId) {
        Account targetAccount = accountRepository.findByIdAndOwner_id(accountId, clientId).orElseThrow(() -> {
            log.warn("Changing account currency failed: Account with id {} and owners id {} not found", accountId, clientId);
            return new NotFoundException("Client not found");
        });
        return targetAccount;
    }

}