package com.expenseiq.controller;

import com.expenseiq.dto.ApiResponse;
import com.expenseiq.model.RecurringTransaction;
import com.expenseiq.service.RecurringTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recurring")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RecurringTransactionController {

    private final RecurringTransactionService recurringService;

    @GetMapping
    public ResponseEntity<List<RecurringTransaction>> getAllRecurring(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return ResponseEntity.ok(recurringService.getUserRecurring(token));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createRecurring(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {
        String token = authHeader.replace("Bearer ", "");
        try {
            RecurringTransaction recurring = recurringService.createRecurring(token, request);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Recurring transaction created successfully")
                    .user(recurring)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteRecurring(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        String token = authHeader.replace("Bearer ", "");
        try {
            recurringService.deleteRecurring(token, id);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Recurring transaction deleted successfully")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }
}
