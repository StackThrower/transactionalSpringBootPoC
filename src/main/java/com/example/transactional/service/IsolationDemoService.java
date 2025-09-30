package com.example.transactional.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

@Service
public class IsolationDemoService {

    private final JdbcTemplate jdbcTemplate;
    private final PlatformTransactionManager txManager;

    public IsolationDemoService(JdbcTemplate jdbcTemplate,
                                @Qualifier("jdbcTxManager") PlatformTransactionManager txManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.txManager = txManager;
    }

    public enum Isolation {
        READ_UNCOMMITTED(TransactionDefinition.ISOLATION_READ_UNCOMMITTED),
        READ_COMMITTED(TransactionDefinition.ISOLATION_READ_COMMITTED),
        REPEATABLE_READ(TransactionDefinition.ISOLATION_REPEATABLE_READ),
        SERIALIZABLE(TransactionDefinition.ISOLATION_SERIALIZABLE);
        public final int level;
        Isolation(int level) { this.level = level; }
        public static Isolation from(String s) {
            Objects.requireNonNull(s, "isolation");
            return Isolation.valueOf(s.trim().toUpperCase());
        }
    }

    public static class NonRepeatableReadResult {
        public final Isolation isolation;
        public final BigDecimal firstRead;
        public final BigDecimal secondRead;
        public final boolean anomaly; // true if values differ
        public NonRepeatableReadResult(Isolation isolation, BigDecimal firstRead, BigDecimal secondRead) {
            this.isolation = isolation;
            this.firstRead = firstRead;
            this.secondRead = secondRead;
            this.anomaly = firstRead != null && secondRead != null && firstRead.compareTo(secondRead) != 0;
        }
    }

    public NonRepeatableReadResult demoNonRepeatableRead(String owner, BigDecimal delta, Isolation isolation) {
        // Latches to coordinate threads
        CountDownLatch afterFirstRead = new CountDownLatch(1);
        CountDownLatch writerDone = new CountDownLatch(1);

        // Start writer thread that updates balance after first read
        Thread writer = new Thread(() -> {
            try {
                afterFirstRead.await();
                runInNewTx(Isolation.READ_COMMITTED, () -> {
                    int upd = jdbcTemplate.update("UPDATE accounts SET balance = balance + ? WHERE owner = ?", delta, owner);
                    if (upd != 1) throw new IllegalArgumentException("Account not found: " + owner);
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                writerDone.countDown();
            }
        }, "writer-nonrepeatable");
        writer.start();

        BigDecimal first;
        BigDecimal second;
        // Reader transaction with requested isolation
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setName("nonRepeatableReader");
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        def.setIsolationLevel(isolation.level);
        TransactionStatus status = txManager.getTransaction(def);
        try {
            first = readBalance(owner);
            afterFirstRead.countDown();
            try { writerDone.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            second = readBalance(owner);
            txManager.commit(status);
        } catch (RuntimeException ex) {
            txManager.rollback(status);
            throw ex;
        }
        return new NonRepeatableReadResult(isolation, first, second);
    }

    public static class PhantomReadResult {
        public final Isolation isolation;
        public final int firstCount;
        public final int secondCount;
        public final boolean anomaly; // true if counts differ
        public PhantomReadResult(Isolation isolation, int firstCount, int secondCount) {
            this.isolation = isolation;
            this.firstCount = firstCount;
            this.secondCount = secondCount;
            this.anomaly = firstCount != secondCount;
        }
    }

    public PhantomReadResult demoPhantomRead(BigDecimal threshold, Isolation isolation) {
        CountDownLatch afterFirstQuery = new CountDownLatch(1);
        CountDownLatch writerDone = new CountDownLatch(1);

        Thread writer = new Thread(() -> {
            try {
                afterFirstQuery.await();
                runInNewTx(Isolation.READ_COMMITTED, () -> {
                    String owner = "phantom-" + UUID.randomUUID();
                    jdbcTemplate.update("INSERT INTO accounts(owner, balance) VALUES (?, ?)", owner, threshold.add(new BigDecimal("1.00")));
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                writerDone.countDown();
            }
        }, "writer-phantom");
        writer.start();

        int c1;
        int c2;
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setName("phantomReader");
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        def.setIsolationLevel(isolation.level);
        TransactionStatus status = txManager.getTransaction(def);
        try {
            c1 = countAbove(threshold);
            afterFirstQuery.countDown();
            try { writerDone.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            c2 = countAbove(threshold);
            txManager.commit(status);
        } catch (RuntimeException ex) {
            txManager.rollback(status);
            throw ex;
        }
        return new PhantomReadResult(isolation, c1, c2);
    }

    private BigDecimal readBalance(String owner) {
        return jdbcTemplate.queryForObject("SELECT balance FROM accounts WHERE owner = ?", (rs, rn) -> rs.getBigDecimal(1), owner);
    }

    private int countAbove(BigDecimal threshold) {
        Integer cnt = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM accounts WHERE balance >= ?", Integer.class, threshold);
        return cnt == null ? 0 : cnt;
    }

    private void runInNewTx(Isolation isolation, Runnable body) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setName("writer");
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        def.setIsolationLevel(isolation.level);
        TransactionStatus status = txManager.getTransaction(def);
        try {
            body.run();
            txManager.commit(status);
        } catch (RuntimeException ex) {
            txManager.rollback(status);
            throw ex;
        }
    }
}

