package com.example.transactional.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Service
public class JdbcTransferService {

    private final JdbcTemplate jdbcTemplate;
    private final PlatformTransactionManager txManager;
    private final DataSource dataSource;

    public JdbcTransferService(JdbcTemplate jdbcTemplate,
                               @Qualifier("jdbcTxManager") PlatformTransactionManager txManager,
                               DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.txManager = txManager;
        this.dataSource = dataSource;
    }

    // Programmatic transaction using PlatformTransactionManager
    public void transferWithTxManager(String fromOwner, String toOwner, BigDecimal amount, boolean failMidway) {
        validateAmount(amount);
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setName("jdbcTransfer");
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus status = txManager.getTransaction(def);
        try {
            debit(fromOwner, amount);
            if (failMidway) {
                throw new RuntimeException("Simulated failure between debit and credit (txManager)");
            }
            credit(toOwner, amount);
            txManager.commit(status);
        } catch (RuntimeException ex) {
            txManager.rollback(status);
            throw ex;
        }
    }

    // Manual JDBC transaction using Connection commit/rollback directly
    public void transferManualConnection(String fromOwner, String toOwner, BigDecimal amount, boolean failMidway) {
        validateAmount(amount);
        try (Connection conn = dataSource.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement("UPDATE accounts SET balance = balance - ? WHERE owner = ?")) {
                    ps.setBigDecimal(1, amount);
                    ps.setString(2, fromOwner);
                    int updated = ps.executeUpdate();
                    if (updated != 1) throw new IllegalArgumentException("Sender not found: " + fromOwner);
                }
                if (failMidway) {
                    throw new RuntimeException("Simulated failure between debit and credit (manual)");
                }
                try (PreparedStatement ps = conn.prepareStatement("UPDATE accounts SET balance = balance + ? WHERE owner = ?")) {
                    ps.setBigDecimal(1, amount);
                    ps.setString(2, toOwner);
                    int updated = ps.executeUpdate();
                    if (updated != 1) throw new IllegalArgumentException("Recipient not found: " + toOwner);
                }
                conn.commit();
            } catch (RuntimeException | SQLException ex) {
                try { conn.rollback(); } catch (SQLException ignore) {}
                throw (ex instanceof RuntimeException) ? (RuntimeException) ex : new RuntimeException(ex);
            } finally {
                try { conn.setAutoCommit(originalAutoCommit); } catch (SQLException ignore) {}
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // No transaction: demonstrates partial update on failure
    public void transferWithoutTransaction(String fromOwner, String toOwner, BigDecimal amount, boolean failMidway) {
        validateAmount(amount);
        debit(fromOwner, amount); // autocommit true by default
        if (failMidway) {
            throw new RuntimeException("Simulated failure without transaction");
        }
        credit(toOwner, amount);
    }

    public BigDecimal balanceOf(String owner) {
        return jdbcTemplate.queryForObject("SELECT balance FROM accounts WHERE owner = ?",
                (rs, rowNum) -> rs.getBigDecimal(1), owner);
    }

    private void debit(String owner, BigDecimal amount) {
        int updated = jdbcTemplate.update("UPDATE accounts SET balance = balance - ? WHERE owner = ?", amount, owner);
        if (updated != 1) throw new IllegalArgumentException("Account not found: " + owner);
    }

    private void credit(String owner, BigDecimal amount) {
        int updated = jdbcTemplate.update("UPDATE accounts SET balance = balance + ? WHERE owner = ?", amount, owner);
        if (updated != 1) throw new IllegalArgumentException("Account not found: " + owner);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }
}
