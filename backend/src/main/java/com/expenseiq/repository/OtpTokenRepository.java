package com.expenseiq.repository;

import com.expenseiq.model.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findTopByIdentifierAndUsedFalseOrderByExpiresAtDesc(String identifier);
}