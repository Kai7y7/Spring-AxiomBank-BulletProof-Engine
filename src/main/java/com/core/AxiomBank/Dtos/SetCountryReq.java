package com.core.AxiomBank.Dtos;

import com.core.AxiomBank.Entities.Country;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class SetCountryReq {

    @NotNull
    @Valid
    private Country country;
}
