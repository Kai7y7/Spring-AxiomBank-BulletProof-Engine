package com.core.AxiomBank.Repositories;

import com.core.AxiomBank.Entities.Account;
import com.core.AxiomBank.Entities.AccountStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Account> findByIdAndOwner_id(Long id,Long ownerId);
    @Modifying
    @Query("UPDATE Account a SET a.status = :status WHERE a.owner.id = :clientId")
    void updateStatusByClientId(AccountStatus status, Long clientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithLock(@Param("id") Long id);


}
