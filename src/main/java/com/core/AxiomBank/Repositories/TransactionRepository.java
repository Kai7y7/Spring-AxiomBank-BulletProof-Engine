package com.core.AxiomBank.Repositories;


import com.core.AxiomBank.Entities.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
