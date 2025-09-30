package com.example.transactional.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class JdbcTransferServiceIT {

    @Autowired
    JdbcTransferService jdbcTransferService;

    @Test
    void txManager_commitsOnSuccess() {
        jdbcTransferService.transferWithTxManager("alice", "bob", new BigDecimal("10.00"), false);
        assertEquals(new BigDecimal("90.00"), jdbcTransferService.balanceOf("alice"));
        assertEquals(new BigDecimal("60.00"), jdbcTransferService.balanceOf("bob"));
    }

    @Test
    void txManager_rollsBackOnFailure() {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                jdbcTransferService.transferWithTxManager("alice", "bob", new BigDecimal("10.00"), true)
        );
        assertTrue(ex.getMessage().contains("Simulated failure"));
        assertEquals(new BigDecimal("100.00"), jdbcTransferService.balanceOf("alice"));
        assertEquals(new BigDecimal("50.00"), jdbcTransferService.balanceOf("bob"));
    }

    @Test
    void manualConnection_commitsOnSuccess() {
        jdbcTransferService.transferManualConnection("alice", "bob", new BigDecimal("25.00"), false);
        assertEquals(new BigDecimal("75.00"), jdbcTransferService.balanceOf("alice"));
        assertEquals(new BigDecimal("75.00"), jdbcTransferService.balanceOf("bob"));
    }

    @Test
    void manualConnection_rollsBackOnFailure() {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                jdbcTransferService.transferManualConnection("alice", "bob", new BigDecimal("25.00"), true)
        );
        assertTrue(ex.getMessage().contains("Simulated failure"));
        assertEquals(new BigDecimal("100.00"), jdbcTransferService.balanceOf("alice"));
        assertEquals(new BigDecimal("50.00"), jdbcTransferService.balanceOf("bob"));
    }

    @Test
    void noTransaction_partialUpdateOnFailure() {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                jdbcTransferService.transferWithoutTransaction("alice", "bob", new BigDecimal("10.00"), true)
        );
        assertTrue(ex.getMessage().contains("failure without transaction"));
        // Partial update: alice debited, bob unchanged
        assertEquals(new BigDecimal("90.00"), jdbcTransferService.balanceOf("alice"));
        assertEquals(new BigDecimal("50.00"), jdbcTransferService.balanceOf("bob"));
    }
}

