package com.core.AxiomBank.Services;


import com.core.AxiomBank.Entities.Country;
import com.core.AxiomBank.Entities.Currency;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class GeneralMethodsService {

    public static String generateIban(Country country) {
        String countryCode = country.name();

        StringBuilder randomNumbers = new StringBuilder();
        SecureRandom random = new SecureRandom();

        for (int i = 0; i < 18; i++) {
            randomNumbers.append(random.nextInt(10));
        }

        return countryCode + "00" + randomNumbers.toString();

    }

    public static Currency determineDefaultCurrency(Country country) {
        return switch (country) {
            case US -> Currency.USD;
            case GB -> Currency.GBP;
            case DE, FR, IT, ES, NL -> Currency.EUR;
            case CH -> Currency.CHF;
            case PL -> Currency.PLN;
            default -> Currency.EUR;
        };
    }




}
