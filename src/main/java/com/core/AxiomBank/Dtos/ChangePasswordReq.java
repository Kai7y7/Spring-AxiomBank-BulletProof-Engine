package com.core.AxiomBank.Dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordReq {

    @NotBlank
    private String oldPassword;

    @NotBlank
    private String newPassword;

}
