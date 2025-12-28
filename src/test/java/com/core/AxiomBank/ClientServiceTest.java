package com.core.AxiomBank;


import com.core.AxiomBank.Dtos.ChangeEmail;
import com.core.AxiomBank.Dtos.ClientResp;
import com.core.AxiomBank.Dtos.CreateClientReq;
import com.core.AxiomBank.Dtos.LoginClientReq;
import com.core.AxiomBank.Entities.*;
import com.core.AxiomBank.Exceptions.BadRequestException;
import com.core.AxiomBank.Redis.RateLimitingService;
import com.core.AxiomBank.Repositories.AccountRepository;
import com.core.AxiomBank.Repositories.ClientRepository;
import com.core.AxiomBank.Repositories.Mappers;
import com.core.AxiomBank.Services.ClientService;
import com.core.AxiomBank.Services.GeneralMethodsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ClientRepository clientRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private Mappers mappers;
    @Mock private RateLimitingService rateLimiterService;

    @InjectMocks
    private ClientService clientService;

    // --- CREATE CLIENT ---

    @Test
    @DisplayName("CREATE: Happy Path - New user, new default account")
    void testCreateClient_Success() {
        CreateClientReq req = new CreateClientReq();
        req.setEmail("test@axiom.com");
        req.setPassword("secure123");
        req.setCountry(Country.US);

        when(clientRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(req.getPassword())).thenReturn("encodedHash");
        when(clientRepository.save(any(Client.class))).thenAnswer(i -> {
            Client c = i.getArgument(0);
            c.setId(1L); // Simulate DB ID generation
            return c;
        });
        when(mappers.toClientDto(any())).thenReturn(new ClientResp());

        // Mock static for default account creation inside the method
        try (MockedStatic<GeneralMethodsService> mockedStatic = mockStatic(GeneralMethodsService.class)) {
            mockedStatic.when(() -> GeneralMethodsService.determineDefaultCurrency(Country.US)).thenReturn(Currency.USD);
            mockedStatic.when(() -> GeneralMethodsService.generateIban(Country.US)).thenReturn("US000");

            clientService.createClient(req);

            // Verify Client was saved with PENDING status
            verify(clientRepository).save(argThat(c ->
                    c.getClientStatus() == ClientStatus.PENDING &&
                            c.getPassword().equals("encodedHash")
            ));

            // Verify Default Account was created
            verify(accountRepository).save(any(Account.class));
        }
    }

    @Test
    @DisplayName("CREATE: Fail - Email already taken")
    void testCreateClient_EmailExists() {
        CreateClientReq req = new CreateClientReq();
        req.setEmail("taken@axiom.com");

        when(clientRepository.existsByEmail("taken@axiom.com")).thenReturn(true);

        assertThatThrownBy(() -> clientService.createClient(req))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // --- LOGIN / VALIDATE ---

    @Test
    @DisplayName("LOGIN: Success - Rate limit pass, Password match")
    void testValidateCredentials_Success() {
        LoginClientReq req = new LoginClientReq("user@axiom.com", "pass");
        Client client = new Client();
        client.setPassword("encodedPass");

        when(clientRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(client));
        when(passwordEncoder.matches("pass", "encodedPass")).thenReturn(true);

        // Should not throw exception
        clientService.validateCredentials(req);

        verify(rateLimiterService).checkLoginAttempts(req.getEmail());
    }

    @Test
    @DisplayName("LOGIN: Fail - Wrong Password")
    void testValidateCredentials_WrongPassword() {
        LoginClientReq req = new LoginClientReq("user@axiom.com", "wrong");
        Client client = new Client();
        client.setPassword("encodedPass");

        when(clientRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(client));
        when(passwordEncoder.matches("wrong", "encodedPass")).thenReturn(false);

        assertThatThrownBy(() -> clientService.validateCredentials(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid credentials");
    }

    // --- VERIFY CLIENT  ---

    @Test
    @DisplayName("VERIFY: Activates Client and Pending Accounts")
    void testVerifyClient() {
        Client client = new Client();
        client.setClientStatus(ClientStatus.PENDING);
        client.setAccounts(new ArrayList<>());

        Account pendingAcc = new Account();
        pendingAcc.setStatus(AccountStatus.PENDING);
        client.getAccounts().add(pendingAcc);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        clientService.verifyClient(1L);

        assertThat(client.getClientStatus()).isEqualTo(ClientStatus.ACTIVE);
        assertThat(pendingAcc.getStatus()).isEqualTo(AccountStatus.ACTIVE);

        verify(clientRepository).save(client);
    }

    // --- CHANGE SENSITIVE DATA ---

    @Test
    @DisplayName("CHANGE EMAIL: Success - Verifies old password first")
    void testChangeEmail_Success() {
        ChangeEmail req = new ChangeEmail();
        req.setEmail("new@axiom.com");
        req.setOldPassword("correctPass");

        Client client = new Client();
        client.setPassword("encodedCorrect");

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(passwordEncoder.matches("correctPass", "encodedCorrect")).thenReturn(true);

        clientService.changeEmail(req, 1L);

        assertThat(client.getEmail()).isEqualTo("new@axiom.com");
    }

    @Test
    @DisplayName("CHANGE EMAIL: Fail - Password verification failed")
    void testChangeEmail_BadPassword() {
        ChangeEmail req = new ChangeEmail();
        req.setEmail("valid@email.com");
        req.setOldPassword("wrong");

        Client client = new Client();
        client.setPassword("encodedCorrect");

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        when(passwordEncoder.matches("wrong", "encodedCorrect")).thenReturn(false);

        assertThatThrownBy(() -> clientService.changeEmail(req, 1L))
                .isInstanceOf(BadRequestException.class);
    }
    @Test
    @DisplayName("ATM: Generate Test ATM Client")
    void testCreateTestAtm() {
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        when(clientRepository.save(any(Client.class))).thenAnswer(i -> i.getArguments()[0]);

        Client atm = clientService.createTestAtm();

        assertThat(atm.getClientRole()).isEqualTo(ClientRole.ATM);
        assertThat(atm.getEmail()).startsWith("accessAtm_");
    }
}