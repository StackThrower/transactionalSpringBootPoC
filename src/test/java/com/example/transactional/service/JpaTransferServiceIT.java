package com.example.transactional.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class JpaTransferServiceIT {

    @Autowired
    JpaTransferService jpaTransferService;

    @Test
    void jpaTransfer_commitsOnSuccess() {
        jpaTransferService.transfer("alice", "bob", new BigDecimal("10.00"), false);
        assertEquals(new BigDecimal("90.00"), jpaTransferService.balanceOf("alice"));
        assertEquals(new BigDecimal("60.00"), jpaTransferService.balanceOf("bob"));
    }

    @Test
    void jpaTransfer_rollsBackOnException() {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                jpaTransferService.transfer("alice", "bob", new BigDecimal("10.00"), true)
        );
        assertTrue(ex.getMessage().contains("Simulated failure"));
        assertEquals(new BigDecimal("100.00"), jpaTransferService.balanceOf("alice"));
        assertEquals(new BigDecimal("50.00"), jpaTransferService.balanceOf("bob"));
    }
}
