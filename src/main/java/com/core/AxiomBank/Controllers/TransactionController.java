package com.core.AxiomBank.Controllers;
import com.core.AxiomBank.Dtos.SetAmountReq;
import com.core.AxiomBank.Dtos.TransactionResp;
import com.core.AxiomBank.Services.TransactionService;
import com.core.AxiomBank.security.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Making money disappear here and reappear there.")
public class TransactionController {

    private final TransactionService transactionService;

    @Operation(summary = "ATM Deposit", description = "Feed the machine. Requires ROLE_ATM.")
    @PreAuthorize("hasRole('ATM')")
    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<TransactionResp> depositMoney(
            @PathVariable Long accountId,
            @Valid @RequestBody SetAmountReq setAmountReq,
            @Parameter(hidden = true) Authentication auth) {
        return ResponseEntity.ok(transactionService.depositTransaction(accountId, setAmountReq, getPrincipal(auth).getId()));
    }

    @Operation(summary = "Withdrawal", description = "Attempt to take out physical cash before the bank run starts.")
    @PostMapping("/{accountId}/withdraw")
    public ResponseEntity<TransactionResp> withdrawMoney(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable Long accountId,
            @Valid @RequestBody SetAmountReq setAmountReq) {
        Long clientId = getPrincipal(authentication).getId();
        return ResponseEntity.ok(transactionService.withdrawTransaction(clientId, accountId, setAmountReq));
    }

    @Operation(summary = "Internal Transfer", description = "Transfer funds to another account so someone else can pay their rent.")
    @PostMapping("/{fromAccountId}/transfer/{toAccountId}")
    public ResponseEntity<TransactionResp> transferMoney(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable Long fromAccountId,
            @PathVariable Long toAccountId,
            @Valid @RequestBody SetAmountReq setAmountReq) {
        Long ownerId = getPrincipal(authentication).getId();
        return ResponseEntity.ok(transactionService.transferTransaction(fromAccountId, ownerId, toAccountId, setAmountReq));
    }

    private UserDetailsImpl getPrincipal(Authentication authentication) {
        return (UserDetailsImpl) authentication.getPrincipal();
    }
}