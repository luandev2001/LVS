package com.example.lab.payment.controller;

import com.example.lab.payment.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class PaymentExceptionHandler {
    @ExceptionHandler(PaymentService.PaymentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Map<String, String> notFound(RuntimeException e) {
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, String> badRequest(RuntimeException e) {
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String, String> conflict(RuntimeException e) {
        return Map.of("error", e.getMessage());
    }
}
