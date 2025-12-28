package com.core.AxiomBank.Dtos;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangeEmail {

   @Email(message = "Email is invalid format")
   @NotBlank
   private String email;

   @NotBlank
   private String oldPassword;


}
