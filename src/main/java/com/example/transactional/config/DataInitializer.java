package com.example.transactional.config;

import com.example.transactional.account.Account;
import com.example.transactional.account.AccountRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class DataInitializer {

    @Bean
    ApplicationRunner initData(AccountRepository repo) {
        return args -> {
            repo.findByOwner("alice").orElseGet(() -> repo.save(new Account("alice", new BigDecimal("100.00"))));
            repo.findByOwner("bob").orElseGet(() -> repo.save(new Account("bob", new BigDecimal("50.00"))));
        };
    }
}

