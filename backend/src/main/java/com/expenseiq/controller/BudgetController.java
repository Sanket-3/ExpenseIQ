package com.expenseiq.controller;

import com.expenseiq.dto.ApiResponse;
import com.expenseiq.model.Budget;
import com.expenseiq.service.BudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    public ResponseEntity<List<Budget>> getAllBudgets(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return ResponseEntity.ok(budgetService.getUserBudgets(token));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> saveBudget(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {
        String token = authHeader.replace("Bearer ", "");
        try {
            Budget budget = budgetService.saveBudget(token, request);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Budget saved successfully")
                    .user(budget)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/{category}")
    public ResponseEntity<ApiResponse> deleteBudget(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String category) {
        String token = authHeader.replace("Bearer ", "");
        try {
            budgetService.deleteBudget(token, category);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Budget deleted successfully")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }
}
