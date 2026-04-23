package com.expenseiq.controller;

import com.expenseiq.dto.ApiResponse;
import com.expenseiq.model.Transaction;
import com.expenseiq.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * TransactionController - REST API for CRUD operations on transactions.
 * 
 * OOP Concepts:
 * - ABSTRACTION: Clean API endpoints hide complex database operations
 * - ENCAPSULATION: Business logic is delegated to TransactionService
 * - POLYMORPHISM: Handles both "expense" and "income" transaction types uniformly
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TransactionController {

    private final TransactionService transactionService;

    // ===== GET ALL TRANSACTIONS FOR A USER =====
    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        List<Transaction> transactions = transactionService.getUserTransactions(token);
        return ResponseEntity.ok(transactions);
    }

    // ===== CREATE TRANSACTION =====
    @PostMapping
    public ResponseEntity<ApiResponse> createTransaction(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {

        String token = authHeader.replace("Bearer ", "");

        try {
            Transaction transaction = transactionService.createTransaction(token, request);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Transaction created successfully")
                    .user(transaction)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    // ===== UPDATE TRANSACTION =====
    @PutMapping("/{transactionId}")
    public ResponseEntity<ApiResponse> updateTransaction(
            @PathVariable Long transactionId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {

        String token = authHeader.replace("Bearer ", "");

        try {
            Transaction transaction = transactionService.updateTransaction(token, transactionId, request);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Transaction updated successfully")
                    .user(transaction)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    // ===== DELETE TRANSACTION =====
    @DeleteMapping("/{transactionId}")
    public ResponseEntity<ApiResponse> deleteTransaction(
            @PathVariable Long transactionId,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");

        try {
            transactionService.deleteTransaction(token, transactionId);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Transaction deleted successfully")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    // ===== GET MONTHLY SUMMARY =====
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getMonthlySummary(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String month) {

        String token = authHeader.replace("Bearer ", "");

        try {
            Map<String, Object> summary = transactionService.getMonthlySummary(token, month);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ===== GET CATEGORY BREAKDOWN =====
    @GetMapping("/categories")
    public ResponseEntity<Map<String, Object>> getCategoryBreakdown(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String month) {

        String token = authHeader.replace("Bearer ", "");

        try {
            Map<String, Object> breakdown = transactionService.getCategoryBreakdown(token, month);
            return ResponseEntity.ok(breakdown);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ===== EXPORT TRANSACTIONS AS CSV =====
    @GetMapping("/export")
    public ResponseEntity<String> exportCSV(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");

        try {
            String csv = transactionService.exportToCSV(token);
            return ResponseEntity.ok()
                    .header("Content-Type", "text/csv")
                    .header("Content-Disposition", "attachment; filename=transactions.csv")
                    .body(csv);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Export failed: " + e.getMessage());
        }
    }
}