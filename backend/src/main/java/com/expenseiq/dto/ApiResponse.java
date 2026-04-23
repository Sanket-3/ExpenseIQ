package com.expenseiq.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApiResponse {
    private boolean success;
    private String message;
    private String token;
    private Object user;
    private boolean needsVerification;
}