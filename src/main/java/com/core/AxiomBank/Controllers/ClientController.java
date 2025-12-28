package com.core.AxiomBank.Controllers;


import com.core.AxiomBank.Dtos.*;
import com.core.AxiomBank.Entities.Client;
import com.core.AxiomBank.Entities.Country;
import com.core.AxiomBank.Services.ClientService;
import com.core.AxiomBank.security.JwtCore;
import com.core.AxiomBank.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import io.swagger.v3.oas.annotations.Parameter;


@RestController
@RequestMapping("/api/clients")
@Tag(name = "Client Management", description = "Endpoints for managing the poor who use this service.")
public class ClientController {

    private final ClientService clientService;
    private final JwtCore jwtCore;

    public ClientController(ClientService clientService, JwtCore jwtCore) {
        this.clientService = clientService;
        this.jwtCore = jwtCore;
    }

    @Operation(summary = "Create client", description = "Onboard a new victim into our database.")
    @PostMapping
    public ResponseEntity<ClientResp> createClient(@Valid @RequestBody CreateClientReq createClientReq) {
        return ResponseEntity.ok(clientService.createClient(createClientReq));
    }

    @Operation(summary = "Login", description = "Authenticate and receive a cookie that tracks your every move.")
    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginClientReq loginClientReq, HttpServletResponse servletResponse) {
        clientService.validateCredentials(loginClientReq);

        Cookie cookie = new Cookie("access-token", jwtCore.generateJwt(loginClientReq.getEmail()));
        cookie.setPath("/");
        cookie.setMaxAge(3600);
        cookie.setAttribute("SameSite", "Lax");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        servletResponse.addCookie(cookie);

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get current client", description = "Retrieve your profile. Try not to cry at the balance.")
    @GetMapping
    public ResponseEntity<ClientResp> getClient(@Parameter(hidden = true) Authentication authentication) {
        return ResponseEntity.ok(clientService.getClientById(getPrincipal(authentication).getId()));
    }

    @Operation(summary = "Change password", description = "Update your password to something 'P@ssword123' again.")
    @PatchMapping("/password")
    public ResponseEntity<ClientResp> changeClientsPassword(@Valid @RequestBody ChangePasswordReq changePasswordReq, @Parameter(hidden = true) Authentication authentication) {
        return ResponseEntity.ok(clientService.changePassword(changePasswordReq, getPrincipal(authentication).getId()));
    }

    @Operation(summary = "Verify client", description = "Confirm the client is a real person.")
    @PatchMapping("/verify")
    public ResponseEntity<Void> verifyClient(@Parameter(hidden = true) Authentication authentication) {
        clientService.verifyClient(getPrincipal(authentication).getId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Create Test ATM")
    @PostMapping("/createAtm")
    public ResponseEntity<String> verifyClient(){
        Client client = clientService.createTestAtm();
        return ResponseEntity.ok( "You can login using this email: "+ client.getEmail() + " and this password: password ");
    }

    @Operation(summary = "Update country", description = "Updates the residential country of the authenticated client.")
    @PatchMapping("/country")
    public ResponseEntity<ClientResp> changeCountry(
            @RequestParam Country country,
            @Parameter(hidden = true) Authentication authentication) {

        Long clientId = getPrincipal(authentication).getId();
        return ResponseEntity.ok(clientService.changeCountry(country, clientId));
    }

    private UserDetailsImpl getPrincipal(Authentication authentication) {
        return (UserDetailsImpl) authentication.getPrincipal();
    }

    @Operation(
            summary = "Change email",
            description = "Updates the client's email address. Requires the current password for security verification."
    )
    @PatchMapping("/email")
    public ResponseEntity<ClientResp> changeEmail(
            @Valid @RequestBody ChangeEmail req,
            @Parameter(hidden = true) Authentication authentication) {

        Long clientId = getPrincipal(authentication).getId();
        return ResponseEntity.ok(clientService.changeEmail(req, clientId));
    }




}