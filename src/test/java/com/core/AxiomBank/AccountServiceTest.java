package com.core.AxiomBank;
import com.core.AxiomBank.Dtos.AccountResp;
import com.core.AxiomBank.Dtos.SetCurrencyReq;
import com.core.AxiomBank.Entities.*;
import com.core.AxiomBank.Exceptions.BadRequestException;
import com.core.AxiomBank.Exceptions.ForbiddenActionException;
import com.core.AxiomBank.Exceptions.NotFoundException;
import com.core.AxiomBank.Repositories.AccountRepository;
import com.core.AxiomBank.Repositories.ClientRepository;
import com.core.AxiomBank.Repositories.Mappers;
import com.core.AxiomBank.Services.AccountService;
import com.core.AxiomBank.Services.GeneralMethodsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private Mappers mappers;

    @InjectMocks
    private AccountService accountService;

    // --- REGISTER NEW ACCOUNT TESTS ---

    @Test
    @DisplayName("REGISTER: Success - Verified client opens an account")
    void testRegister_Success() {
        Long clientId = 1L;
        SetCurrencyReq req = new SetCurrencyReq();
        req.setCurrency(Currency.USD);

        Client activeClient = new Client();
        activeClient.setId(clientId);
        activeClient.setClientStatus(ClientStatus.ACTIVE);
        activeClient.setCountry(Country.US);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(activeClient));
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArguments()[0]);
        when(mappers.toAccountDto(any())).thenReturn(new AccountResp());


        try (MockedStatic<GeneralMethodsService> mockedStatic = mockStatic(GeneralMethodsService.class)) {
            mockedStatic.when(() -> GeneralMethodsService.generateIban(Country.US)).thenReturn("US123456");

            accountService.registerNewAccount(req, clientId);

            verify(accountRepository).save(argThat(acc ->
                    acc.getBalance().compareTo(BigDecimal.ZERO) == 0 &&
                            acc.getStatus() == AccountStatus.ACTIVE &&
                            acc.getCurrency() == Currency.USD &&
                            acc.getIban().equals("US123456")
            ));
        }
    }

    @Test
    @DisplayName("REGISTER: Security Block - Pending/Banned client tries to open account")
    void testRegister_SecurityBlock() {
        Long clientId = 666L;
        SetCurrencyReq req = new SetCurrencyReq();
        req.setCurrency(Currency.EUR);

        Client shadyClient = new Client();
        shadyClient.setId(clientId);
        shadyClient.setClientStatus(ClientStatus.PENDING);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(shadyClient));

        assertThatThrownBy(() -> accountService.registerNewAccount(req, clientId))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessageContaining("not active");

        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("REGISTER: Ghost Client - ID doesn't exist")
    void testRegister_ClientNotFound() {
        when(clientRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.registerNewAccount(new SetCurrencyReq(), 999L))
                .isInstanceOf(NotFoundException.class);
    }

    // --- CHANGE CURRENCY TESTS ---

    @Test
    @DisplayName("CURRENCY: Success - Account is empty, switch allowed")
    void testChangeCurrency_Success() {
        Long accId = 100L;
        Long ownerId = 1L;
        SetCurrencyReq req = new SetCurrencyReq();
        req.setCurrency(Currency.EUR);

        Account account = new Account();
        account.setBalance(BigDecimal.ZERO); // Empty!
        account.setCurrency(Currency.USD);

        when(accountRepository.findByIdAndOwner_id(accId, ownerId)).thenReturn(Optional.of(account));
        when(mappers.toAccountDto(any())).thenReturn(new AccountResp());

        accountService.changeAccountCurrency(accId, req, ownerId);

        assertThat(account.getCurrency()).isEqualTo(Currency.EUR);
    }

    @Test
    @DisplayName("CURRENCY: Fail - Account has money (Financial Protection)")
    void testChangeCurrency_Fail_HasBalance() {
        Long accId = 100L;
        Long ownerId = 1L;

        Account richAccount = new Account();
        richAccount.setBalance(new BigDecimal("0.01")); // Even a penny blocks it

        when(accountRepository.findByIdAndOwner_id(accId, ownerId)).thenReturn(Optional.of(richAccount));

        assertThatThrownBy(() -> accountService.changeAccountCurrency(accId, new SetCurrencyReq(), ownerId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("remaining balance");
    }

    @Test
    @DisplayName("CURRENCY: Security - Trying to change someone else's account")
    void testChangeCurrency_Security_NotOwner() {
        when(accountRepository.findByIdAndOwner_id(100L, 666L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.changeAccountCurrency(100L, new SetCurrencyReq(), 666L))
                .isInstanceOf(NotFoundException.class);
    }

    // --- CLOSE ACCOUNT TESTS ---

    @Test
    @DisplayName("CLOSE: Success - Account empty and clean")
    void testCloseAccount_Success() {
        Account account = new Account();
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(AccountStatus.ACTIVE);

        when(accountRepository.findByIdAndOwner_id(100L, 1L)).thenReturn(Optional.of(account));

        accountService.closeAccount(100L, 1L);

        assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
    }

    @Test
    @DisplayName("CLOSE: Fail - User trying to dash with positive balance")
    void testCloseAccount_Fail_PositiveBalance() {
        Account account = new Account();
        account.setBalance(new BigDecimal("100.00"));

        when(accountRepository.findByIdAndOwner_id(100L, 1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.closeAccount(100L, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Withdraw all funds");
    }
}