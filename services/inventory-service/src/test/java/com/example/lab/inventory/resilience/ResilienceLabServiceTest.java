package com.example.lab.inventory.resilience;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResilienceLabServiceTest {
    @Test void successfulCallReturnsAttemptAndState() {
        var service = new ResilienceLabService(2, 3, 100);
        var result = service.call(false, 0, 0);
        assertEquals("SUCCESS", result.status());
        assertEquals(1, result.attempts());
        assertEquals("CLOSED", result.circuitState());
    }

    @Test void repeatedFailuresOpenCircuitAndRejectNextCall() {
        var service = new ResilienceLabService(2, 2, 10_000);
        assertThrows(IllegalStateException.class, () -> service.call(true, 0, 0));
        assertThrows(IllegalStateException.class, () -> service.call(true, 0, 0));
        assertThrows(ResilienceLabService.CircuitOpenException.class, () -> service.call(false, 0, 0));
    }
}
