package com.example.transactional.web;

import com.example.transactional.service.IsolationDemoService;
import com.example.transactional.service.IsolationDemoService.Isolation;
import com.example.transactional.service.IsolationDemoService.NonRepeatableReadResult;
import com.example.transactional.service.IsolationDemoService.PhantomReadResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/isolation")
public class IsolationController {

    private final IsolationDemoService demoService;

    public IsolationController(IsolationDemoService demoService) {
        this.demoService = demoService;
    }

    @GetMapping("/non-repeatable")
    public ResponseEntity<?> nonRepeatable(@RequestParam(defaultValue = "alice") String owner,
                                           @RequestParam(defaultValue = "5.00") BigDecimal delta,
                                           @RequestParam(defaultValue = "READ_COMMITTED") String level) {
        Isolation iso = Isolation.from(level);
        NonRepeatableReadResult res = demoService.demoNonRepeatableRead(owner, delta, iso);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("isolation", iso.name());
        resp.put("firstRead", res.firstRead);
        resp.put("secondRead", res.secondRead);
        resp.put("anomaly", res.anomaly);
        resp.put("note", "anomaly=true indicates a non-repeatable read occurred");
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/phantom")
    public ResponseEntity<?> phantom(@RequestParam(defaultValue = "50.00") BigDecimal threshold,
                                     @RequestParam(defaultValue = "READ_COMMITTED") String level) {
        Isolation iso = Isolation.from(level);
        PhantomReadResult res = demoService.demoPhantomRead(threshold, iso);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("isolation", iso.name());
        resp.put("firstCount", res.firstCount);
        resp.put("secondCount", res.secondCount);
        resp.put("anomaly", res.anomaly);
        resp.put("note", "anomaly=true indicates a phantom read (row count changed during transaction)");
        return ResponseEntity.ok(resp);
    }
}

