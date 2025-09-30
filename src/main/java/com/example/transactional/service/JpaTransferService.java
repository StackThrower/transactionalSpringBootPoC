package com.example.transactional.service;

import com.example.transactional.account.Account;
import com.example.transactional.account.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class JpaTransferService {

    private final AccountRepository accountRepository;

    public JpaTransferService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public void transfer(String fromOwner, String toOwner, BigDecimal amount, boolean failMidway) {
        validateAmount(amount);
        Account from = accountRepository.findByOwner(fromOwner)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found: " + fromOwner));
        Account to = accountRepository.findByOwner(toOwner)
                .orElseThrow(() -> new IllegalArgumentException("Recipient not found: " + toOwner));

        if (from.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        from.setBalance(from.getBalance().subtract(amount));
        accountRepository.save(from);

        if (failMidway) {
            // Simulate an unexpected failure to demonstrate rollback
            throw new RuntimeException("Simulated failure between operations");
        }

        to.setBalance(to.getBalance().add(amount));
        accountRepository.save(to);
    }

    @Transactional(readOnly = true)
    public BigDecimal balanceOf(String owner) {
        return accountRepository.findByOwner(owner)
                .map(Account::getBalance)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + owner));
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }
}

