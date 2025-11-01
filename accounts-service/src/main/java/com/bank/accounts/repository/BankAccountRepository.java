package com.bank.accounts.repository;

import com.bank.accounts.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    List<BankAccount> findByAccountId(Long accountId);
    Optional<BankAccount> findByIdAndAccountId(Long id, Long accountId);

    @Query("SELECT ba FROM BankAccount ba WHERE ba.account.username = :username")
    List<BankAccount> findByAccountUsername(String username);

    @Query("SELECT ba FROM BankAccount ba WHERE ba.id = :id AND ba.account.username = :username")
    Optional<BankAccount> findByIdAndAccountUsername(Long id, String username);
}
