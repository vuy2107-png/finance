package com.codegym.finance.controller.api;
import com.codegym.finance.entity.category.Category;

import com.codegym.finance.entity.transaction.Transaction;
import com.codegym.finance.service.transaction.ITransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardApiController {

    @Autowired
    private ITransactionService transactionService;

    @GetMapping("/category-stats")
    public ResponseEntity<Map<String, Double>> getCategoryStats(Authentication auth) {
        String username = auth.getName();
        return ResponseEntity.ok(transactionService.getCategoryExpenses(username));
    }

    @GetMapping("/monthly-trend")
    public ResponseEntity<Map<String, Object>> getMonthlyTrend(Authentication auth) {
        String username = auth.getName();
        return ResponseEntity.ok(transactionService.getMonthlyTrend(username));
    }

    @GetMapping("/transaction/{id}")
    public ResponseEntity<Transaction> getTransaction(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(transactionService.findById(id, auth.getName()));
    }
}
