package com.example.transactional.web;

import com.example.transactional.service.JdbcTransferService;
import com.example.transactional.service.JpaTransferService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TransferController {

    private final JpaTransferService jpaTransferService;
    private final JdbcTransferService jdbcTransferService;

    public TransferController(JpaTransferService jpaTransferService, JdbcTransferService jdbcTransferService) {
        this.jpaTransferService = jpaTransferService;
        this.jdbcTransferService = jdbcTransferService;
    }

    @GetMapping("/accounts/{owner}/balance")
    public Map<String, Object> balance(@PathVariable String owner) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("owner", owner);
        resp.put("balance", jpaTransferService.balanceOf(owner));
        return resp;
    }

    @PostMapping("/transfer/jpa")
    public ResponseEntity<?> transferJpa(@RequestParam String from,
                                         @RequestParam String to,
                                         @RequestParam BigDecimal amount,
                                         @RequestParam(defaultValue = "false") boolean failMidway) {
        jpaTransferService.transfer(from, to, amount, failMidway);
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "ok");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/transfer/jdbc-txmgr")
    public ResponseEntity<?> transferJdbcTxMgr(@RequestParam String from,
                                               @RequestParam String to,
                                               @RequestParam BigDecimal amount,
                                               @RequestParam(defaultValue = "false") boolean failMidway) {
        jdbcTransferService.transferWithTxManager(from, to, amount, failMidway);
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "ok");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/transfer/jdbc-manual")
    public ResponseEntity<?> transferJdbcManual(@RequestParam String from,
                                                @RequestParam String to,
                                                @RequestParam BigDecimal amount,
                                                @RequestParam(defaultValue = "false") boolean failMidway) {
        jdbcTransferService.transferManualConnection(from, to, amount, failMidway);
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "ok");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/transfer/jdbc-no-tx")
    public ResponseEntity<?> transferJdbcNoTx(@RequestParam String from,
                                              @RequestParam String to,
                                              @RequestParam BigDecimal amount,
                                              @RequestParam(defaultValue = "false") boolean failMidway) {
        jdbcTransferService.transferWithoutTransaction(from, to, amount, failMidway);
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "ok");
        return ResponseEntity.ok(resp);
    }

    @ExceptionHandler({IllegalArgumentException.class, RuntimeException.class})
    public ResponseEntity<?> handle(RuntimeException ex) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("error", ex.getMessage());
        return ResponseEntity.badRequest().body(resp);
    }
}
