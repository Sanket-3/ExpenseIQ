package com.expenseiq.service;

import com.expenseiq.model.Budget;
import com.expenseiq.model.User;
import com.expenseiq.repository.BudgetRepository;
import com.expenseiq.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private User getUserFromToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long userId = Long.parseLong(claims.getSubject());
            return userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
        } catch (Exception e) {
            throw new RuntimeException("Invalid or expired token");
        }
    }

    public List<Budget> getUserBudgets(String token) {
        User user = getUserFromToken(token);
        return budgetRepository.findByUserId(user.getId());
    }

    public Budget saveBudget(String token, Map<String, Object> request) {
        User user = getUserFromToken(token);
        String category = (String) request.get("category");
        BigDecimal amount = new BigDecimal(request.get("amount").toString());

        Optional<Budget> existing = budgetRepository.findByUserIdAndCategory(user.getId(), category);
        
        Budget budget;
        if (existing.isPresent()) {
            budget = existing.get();
            budget.setAmount(amount);
        } else {
            budget = Budget.builder()
                    .user(user)
                    .category(category)
                    .amount(amount)
                    .createdAt(LocalDateTime.now())
                    .build();
        }

        return budgetRepository.save(budget);
    }

    public void deleteBudget(String token, String category) {
        User user = getUserFromToken(token);
        Budget budget = budgetRepository.findByUserIdAndCategory(user.getId(), category)
                .orElseThrow(() -> new RuntimeException("Budget not found for category: " + category));
        budgetRepository.delete(budget);
    }
}
