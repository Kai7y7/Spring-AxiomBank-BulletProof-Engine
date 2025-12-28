package com.core.AxiomBank.Dtos;



import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginClientReq {

    @NotBlank
    private String email;

    @NotBlank
    private String password;

}
