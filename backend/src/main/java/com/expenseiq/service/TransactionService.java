package com.expenseiq.service;

import com.expenseiq.model.Transaction;
import com.expenseiq.model.User;
import com.expenseiq.repository.TransactionRepository;
import com.expenseiq.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TransactionService — Core business logic for all transaction operations.
 * 
 * OOP Concepts:
 * - ENCAPSULATION: All data manipulation is private/internal
 * - ABSTRACTION: Controller calls simple methods, complexity is hidden here
 * - SINGLE RESPONSIBILITY: Only handles transaction-related logic
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    // ===== EXTRACT USER FROM JWT TOKEN =====
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

    // ===== GET ALL USER TRANSACTIONS =====
    public List<Transaction> getUserTransactions(String token) {
        User user = getUserFromToken(token);
        return transactionRepository.findByUserIdOrderByDateDesc(user.getId());
    }

    // ===== CREATE TRANSACTION =====
    public Transaction createTransaction(String token, Map<String, Object> request) {
        User user = getUserFromToken(token);

        Transaction transaction = Transaction.builder()
                .user(user)
                .name((String) request.get("name"))
                .amount(new BigDecimal(request.get("amount").toString()))
                .type((String) request.get("type"))
                .category((String) request.get("category"))
                .date(LocalDate.parse((String) request.get("date")))
                .tags(request.get("tags") != null ? request.get("tags").toString() : null)
                .notes(request.get("notes") != null ? (String) request.get("notes") : null)
                .createdAt(LocalDateTime.now())
                .build();

        return transactionRepository.save(transaction);
    }

    // ===== UPDATE TRANSACTION =====
    public Transaction updateTransaction(String token, Long transactionId, Map<String, Object> request) {
        User user = getUserFromToken(token);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        // Security check: ensure user owns this transaction
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized: You don't own this transaction");
        }

        // Update fields
        if (request.containsKey("name")) transaction.setName((String) request.get("name"));
        if (request.containsKey("amount")) transaction.setAmount(new BigDecimal(request.get("amount").toString()));
        if (request.containsKey("type")) transaction.setType((String) request.get("type"));
        if (request.containsKey("category")) transaction.setCategory((String) request.get("category"));
        if (request.containsKey("date")) transaction.setDate(LocalDate.parse((String) request.get("date")));
        if (request.containsKey("tags")) transaction.setTags(request.get("tags").toString());
        if (request.containsKey("notes")) transaction.setNotes((String) request.get("notes"));

        return transactionRepository.save(transaction);
    }

    // ===== DELETE TRANSACTION =====
    public void deleteTransaction(String token, Long transactionId) {
        User user = getUserFromToken(token);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        // Security check
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized: You don't own this transaction");
        }

        transactionRepository.delete(transaction);
    }

    // ===== MONTHLY SUMMARY =====
    public Map<String, Object> getMonthlySummary(String token, String monthStr) {
        User user = getUserFromToken(token);
        List<Transaction> allTransactions = transactionRepository.findByUserIdOrderByDateDesc(user.getId());

        YearMonth targetMonth = monthStr != null
                ? YearMonth.parse(monthStr)
                : YearMonth.now();

        List<Transaction> monthlyTransactions = allTransactions.stream()
                .filter(t -> YearMonth.from(t.getDate()).equals(targetMonth))
                .collect(Collectors.toList());

        BigDecimal totalIncome = monthlyTransactions.stream()
                .filter(t -> "income".equals(t.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = monthlyTransactions.stream()
                .filter(t -> "expense".equals(t.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal balance = totalIncome.subtract(totalExpense);

        // Total balance across all time
        BigDecimal allTimeIncome = allTransactions.stream()
                .filter(t -> "income".equals(t.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal allTimeExpense = allTransactions.stream()
                .filter(t -> "expense".equals(t.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> summary = new HashMap<>();
        summary.put("month", targetMonth.toString());
        summary.put("monthlyIncome", totalIncome);
        summary.put("monthlyExpense", totalExpense);
        summary.put("monthlyBalance", balance);
        summary.put("totalBalance", allTimeIncome.subtract(allTimeExpense));
        summary.put("totalTransactions", allTransactions.size());
        summary.put("monthlyTransactionCount", monthlyTransactions.size());

        return summary;
    }

    // ===== CATEGORY BREAKDOWN =====
    public Map<String, Object> getCategoryBreakdown(String token, String monthStr) {
        User user = getUserFromToken(token);
        List<Transaction> allTransactions = transactionRepository.findByUserIdOrderByDateDesc(user.getId());

        YearMonth targetMonth = monthStr != null
                ? YearMonth.parse(monthStr)
                : YearMonth.now();

        Map<String, BigDecimal> categoryTotals = allTransactions.stream()
                .filter(t -> "expense".equals(t.getType()))
                .filter(t -> YearMonth.from(t.getDate()).equals(targetMonth))
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        // Sort by amount descending
        List<Map<String, Object>> categories = categoryTotals.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(entry -> {
                    Map<String, Object> cat = new HashMap<>();
                    cat.put("category", entry.getKey());
                    cat.put("amount", entry.getValue());
                    return cat;
                })
                .collect(Collectors.toList());

        BigDecimal totalExpense = categoryTotals.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Add percentage to each category
        categories.forEach(cat -> {
            BigDecimal amount = (BigDecimal) cat.get("amount");
            double percentage = totalExpense.compareTo(BigDecimal.ZERO) > 0
                    ? amount.divide(totalExpense, 4, java.math.RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .doubleValue()
                    : 0;
            cat.put("percentage", Math.round(percentage * 10.0) / 10.0);
        });

        Map<String, Object> result = new HashMap<>();
        result.put("month", targetMonth.toString());
        result.put("totalExpense", totalExpense);
        result.put("categories", categories);

        return result;
    }

    // ===== EXPORT TO CSV =====
    public String exportToCSV(String token) {
        User user = getUserFromToken(token);
        List<Transaction> transactions = transactionRepository.findByUserIdOrderByDateDesc(user.getId());

        StringBuilder csv = new StringBuilder();
        csv.append("Date,Type,Name,Category,Amount,Tags,Notes\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Transaction t : transactions) {
            csv.append(String.format("%s,%s,\"%s\",\"%s\",%.2f,\"%s\",\"%s\"\n",
                    t.getDate().format(formatter),
                    t.getType(),
                    escapeCsv(t.getName()),
                    escapeCsv(t.getCategory()),
                    t.getAmount(),
                    escapeCsv(t.getTags() != null ? t.getTags() : ""),
                    escapeCsv(t.getNotes() != null ? t.getNotes() : "")
            ));
        }

        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
}