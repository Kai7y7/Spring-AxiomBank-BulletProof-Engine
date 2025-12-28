package com.core.AxiomBank.Services;
import com.core.AxiomBank.Dtos.*;
import com.core.AxiomBank.Entities.*;
import com.core.AxiomBank.Exceptions.BadRequestException;
import com.core.AxiomBank.Exceptions.NotFoundException;
import com.core.AxiomBank.Redis.RateLimitingService;
import com.core.AxiomBank.Repositories.AccountRepository;
import com.core.AxiomBank.Repositories.ClientRepository;
import com.core.AxiomBank.Repositories.Mappers;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Service
@AllArgsConstructor
@Slf4j
public class ClientService {

    private final PasswordEncoder passwordEncoder;
    private final ClientRepository clientRepository;
    private final AccountRepository accountRepository;
    private final Mappers mappers;
    private final RateLimitingService rateLimiterService; // Redis

    @Transactional
    public ClientResp createClient(CreateClientReq req) {
        log.info("Creating new client with email: {}", req.getEmail());

        if(clientRepository.existsByEmail(req.getEmail())) {
            log.warn("Registration failed: Email {} already exists", req.getEmail());
            throw new DataIntegrityViolationException("Email already exists");
        }

        Client client = new Client();
        client.setEmail(req.getEmail());
        client.setPassword(passwordEncoder.encode(req.getPassword()));
        client.setClientStatus(ClientStatus.PENDING);
        client.setCountry(req.getCountry());
        client.setClientRole(ClientRole.USER);


        Client savedClient = clientRepository.save(client);
        createDefaultAccount(savedClient);

        log.info("Client created successfully with ID: {}", savedClient.getId());
        return mappers.toClientDto(savedClient);
    }

    @Transactional(readOnly = true)
    public void validateCredentials(LoginClientReq req) {


        Client targetClient = clientRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));


        rateLimiterService.checkLoginAttempts(req.getEmail());

        if (!passwordEncoder.matches(req.getPassword(), targetClient.getPassword())) {
            log.warn("Failed login attempt for email: {}", req.getEmail());
            throw new BadRequestException("Invalid credentials");
        }
        log.info("Successful login for client: {}", targetClient.getId());
    }

    @Transactional(readOnly = true)
    public ClientResp getClientById(Long id) {
        return mappers.toClientDto(getClient(id));
    }

    @Transactional
    public Client createTestAtm(){
        log.info("Generating Test ATM Client");
        Client client = new Client();
        client.setClientRole(ClientRole.ATM);
        client.setCountry(Country.US);
        client.setClientStatus(ClientStatus.ACTIVE);
        client.setEmail("accessAtm_" + UUID.randomUUID().toString().substring(0,5));
        client.setPassword(passwordEncoder.encode("password"));
        return clientRepository.save(client);
    }

    @Transactional
    public ClientResp changePassword(ChangePasswordReq req, Long id) {
        log.info("Password change request for Client ID: {}", id);
        Client client = getClient(id);

        if(!passwordEncoder.matches(req.getOldPassword(), client.getPassword())){
            throw new BadRequestException("Incorrect old password");
        }

        client.setPassword(passwordEncoder.encode(req.getNewPassword()));
        return mappers.toClientDto(clientRepository.save(client));
    }

    @Transactional
    public ClientResp changeCountry(Country country, Long id) {
        log.info("Country change request for Client ID: {}", id);
        Client client = getClient(id);

        if(country == null){
            throw new BadRequestException("Country field is empty");
        }
        client.setCountry(country);
        return mappers.toClientDto(clientRepository.save(client));
    }

    @Transactional
    public ClientResp changeEmail(ChangeEmail req, Long id) {
        log.info("Email change request for Client ID: {}", id);
        Client client = getClient(id);

        if(clientRepository.existsByEmail(req.getEmail())) {
            log.warn("Email changing failed: Email {} already exists", req.getEmail());
            throw new DataIntegrityViolationException("");
        }

        if(!passwordEncoder.matches(req.getOldPassword(), client.getPassword())){
            throw new BadRequestException("Incorrect password");
        }

        client.setEmail(req.getEmail());
        return mappers.toClientDto(clientRepository.save(client));
    }

    @Transactional
    public void verifyClient(Long clientId) {
        log.info("Verifying Client ID: {}", clientId);
        Client client = getClient(clientId);

        client.setClientStatus(ClientStatus.ACTIVE);


        List<Account> pendingAccounts =
                client.getAccounts()
                .stream()
                .filter(acc -> acc.getStatus() == AccountStatus.PENDING)
                .toList();

        //I could do that with forEach
        for (Account acc : pendingAccounts) {
            acc.setStatus(AccountStatus.ACTIVE);
        }

        clientRepository.save(client);
    }

    @Transactional
    public void closeClientById(Long id) {
        log.warn("CLOSING Client ID: {}", id);
        Client client = getClient(id);
        client.setClientStatus(ClientStatus.CLOSED);
        accountRepository.updateStatusByClientId(AccountStatus.CLOSED, id);
    }

    private void createDefaultAccount(Client client){
        Account account = new Account();
        account.setBalance(BigDecimal.ZERO);
        account.setOwner(client);
        account.setStatus(AccountStatus.PENDING);
        account.setCurrency(GeneralMethodsService.determineDefaultCurrency(client.getCountry()));
        account.setIban(GeneralMethodsService.generateIban(client.getCountry()));

        if (client.getAccounts() == null) {
            client.setAccounts(new ArrayList<>());
        }
        client.getAccounts().add(account);
        accountRepository.save(account);
    }

    private Client getClient(Long id){
        return clientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Client not found"));
    }
}