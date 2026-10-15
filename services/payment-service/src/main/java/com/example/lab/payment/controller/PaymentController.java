package com.example.lab.payment.controller;

import com.example.lab.payment.domain.GatewayMode;
import com.example.lab.payment.domain.Payment;
import com.example.lab.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Payment create(@RequestParam String orderReference,
                          @RequestParam BigDecimal amount,
                          @RequestParam(defaultValue = "VND") String currency,
                          @RequestHeader("Idempotency-Key") String idempotencyKey,
                          @RequestParam(defaultValue = "SUCCESS") GatewayMode mode,
                          @RequestParam(required = false) Long inventoryId,
                          @RequestParam(defaultValue = "1") long quantity,
                          @RequestParam(defaultValue = "NONE") String sagaFailureMode) {
        return service.create(orderReference, amount, currency, idempotencyKey, mode, inventoryId, quantity, sagaFailureMode);
    }

    @GetMapping("/{id}")
    public Payment get(@PathVariable UUID id) { return service.get(id); }

    @PostMapping("/{id}/retry")
    public Payment retry(@PathVariable UUID id, @RequestParam(defaultValue = "SUCCESS") GatewayMode mode) {
        return service.retry(id, mode);
    }

    @PostMapping("/{id}/events/duplicate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void duplicateEvent(@PathVariable UUID id) { service.publishDuplicateEvent(id); }

    @PostMapping("/{id}/refund")
    public Payment refund(@PathVariable UUID id) { return service.refund(id); }

    @PostMapping("/{id}/webhook")
    public PaymentService.CallbackResult webhook(@PathVariable UUID id,
                                                  @RequestParam String gatewayReference,
                                                  @RequestParam BigDecimal amount,
                                                  @RequestParam String currency,
                                                  @RequestParam boolean success,
                                                  @RequestHeader("X-Payment-Signature") String signature) {
        return service.receiveWebhook(id, gatewayReference, amount, currency, success, signature);
    }
}
