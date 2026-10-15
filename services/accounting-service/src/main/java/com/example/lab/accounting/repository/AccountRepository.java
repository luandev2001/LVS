package com.example.lab.accounting.repository;

import com.example.lab.accounting.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByCode(String code);
}
