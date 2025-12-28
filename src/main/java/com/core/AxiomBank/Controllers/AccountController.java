package com.core.AxiomBank.Controllers;


import com.core.AxiomBank.Dtos.AccountResp;
import com.core.AxiomBank.Dtos.SetCurrencyReq;
import com.core.AxiomBank.Services.AccountService;
import com.core.AxiomBank.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Account Management", description = "Everything needed to manage user accounts before the inevitable bankruptcy.")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @Operation(summary = "Register new account", description = "Creates a new account for a client. Warning: Side effects may include extreme poverty.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account created. Welcome to the machine."),
            @ApiResponse(responseCode = "400", description = "Invalid input.")
    })
    @PostMapping
    public ResponseEntity<AccountResp> createNewAccount(
            @Parameter(hidden = true) Authentication authentication,
            @Valid @RequestBody SetCurrencyReq setCurrencyReq) {
        return ResponseEntity.ok(accountService.registerNewAccount(setCurrencyReq, getPrincipal(authentication).getId()));
    }

    @Operation(summary = "Change account currency", description = "Switch to a currency that loses value even faster than the current one.")
    @PatchMapping("/{id}/currency")
    public ResponseEntity<AccountResp> patchAccountsCurrency(
            @PathVariable Long id,
            @Valid @RequestBody SetCurrencyReq setCurrencyReq,
            @Parameter(hidden = true) Authentication authentication) {
        return ResponseEntity.ok(accountService.changeAccountCurrency(id, setCurrencyReq, getPrincipal(authentication).getId()));
    }

    @Operation(summary = "Close account", description = "Terminate the account. We keep the loose change.")
    @PatchMapping("/{id}/close")
    public ResponseEntity<Void> closeAccount(
            @PathVariable Long id,
            @Parameter(hidden = true) Authentication authentication) {
        accountService.closeAccount(id, getPrincipal(authentication).getId());
        return ResponseEntity.ok().build();
    }

    private UserDetailsImpl getPrincipal(Authentication authentication){
        return (UserDetailsImpl) authentication.getPrincipal();
    }
}