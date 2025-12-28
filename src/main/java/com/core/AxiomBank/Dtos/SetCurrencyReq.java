package com.core.AxiomBank.Dtos;


import com.core.AxiomBank.Entities.Currency;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SetCurrencyReq {

    @NotNull
    private Currency currency;

}
