package com.core.AxiomBank.Dtos;


import com.core.AxiomBank.Entities.ClientRole;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ClientResp {
    private Long id;
    private String email;

    private ClientRole clientRole;
    private String country;
    private String clientStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<AccountResp> accounts;
}
