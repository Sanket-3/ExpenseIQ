package com.expenseiq.service;

import com.expenseiq.model.RecurringTransaction;
import com.expenseiq.model.User;
import com.expenseiq.repository.RecurringTransactionRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecurringTransactionService {

    private final RecurringTransactionRepository recurringRepository;
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

    public List<RecurringTransaction> getUserRecurring(String token) {
        User user = getUserFromToken(token);
        return recurringRepository.findByUserId(user.getId());
    }

    public RecurringTransaction createRecurring(String token, Map<String, Object> request) {
        User user = getUserFromToken(token);

        RecurringTransaction recurring = RecurringTransaction.builder()
                .user(user)
                .name((String) request.get("name"))
                .amount(new BigDecimal(request.get("amount").toString()))
                .category((String) request.get("category"))
                .frequency((String) request.get("frequency"))
                .nextDate(LocalDate.parse((String) request.get("nextDate")))
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        return recurringRepository.save(recurring);
    }

    public void deleteRecurring(String token, Long id) {
        User user = getUserFromToken(token);
        RecurringTransaction recurring = recurringRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recurring transaction not found"));

        if (!recurring.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        recurringRepository.delete(recurring);
    }
}
