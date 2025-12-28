package com.core.AxiomBank.Dtos;


import com.core.AxiomBank.Entities.Country;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateClientReq {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 9, max = 100)
    private String password;

    @NotNull
    @Valid
    private Country country;


}
