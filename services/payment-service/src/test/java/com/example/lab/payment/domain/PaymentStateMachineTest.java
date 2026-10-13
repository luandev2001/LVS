package com.example.lab.payment.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class PaymentStateMachineTest {
    @Test
    void successfulPaymentCanBeRefunded() {
        Payment p = Payment.create("ORDER-1", BigDecimal.TEN, "VND", "key-1", "fp-1");
        p.startAttempt("GW-1");
        assertEquals(Payment.CallbackResult.APPLIED, p.applyCallback(true));
        p.refund();
        assertEquals(PaymentStatus.REFUNDED, p.getStatus());
    }
    @Test
    void duplicateSuccessDoesNotApplyTwice() {
        Payment p = Payment.create("ORDER-2", BigDecimal.TEN, "VND", "key-2", "fp-2");
        p.startAttempt("GW-2");
        assertEquals(Payment.CallbackResult.APPLIED, p.applyCallback(true));
        assertEquals(Payment.CallbackResult.DUPLICATE, p.applyCallback(true));
        assertEquals(PaymentStatus.SUCCEEDED, p.getStatus());
    }
    @Test
    void delayedSuccessCanReconcileTimeout() {
        Payment p = Payment.create("ORDER-3", BigDecimal.TEN, "VND", "key-3", "fp-3");
        p.startAttempt("GW-3");
        assertTrue(p.timeoutIfExpired(p.getUpdatedAt().plusSeconds(1), 0));
        assertEquals(PaymentStatus.TIMED_OUT, p.getStatus());
        assertEquals(Payment.CallbackResult.APPLIED, p.applyCallback(true));
        assertEquals(PaymentStatus.SUCCEEDED, p.getStatus());
    }
    @Test
    void invalidTransitionIsRejected() {
        Payment p = Payment.create("ORDER-4", BigDecimal.TEN, "VND", "key-4", "fp-4");
        assertThrows(IllegalStateException.class, p::refund);
    }
}
