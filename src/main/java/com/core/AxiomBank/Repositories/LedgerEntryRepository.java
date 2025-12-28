package com.core.AxiomBank.Repositories;

import com.core.AxiomBank.Entities.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
}
