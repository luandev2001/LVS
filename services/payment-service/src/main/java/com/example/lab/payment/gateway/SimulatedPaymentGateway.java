package com.example.lab.payment.gateway;

import com.example.lab.payment.domain.GatewayMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Component
public class SimulatedPaymentGateway {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final String secret;

    public SimulatedPaymentGateway(@Value("${payment.gateway.webhook-secret}") String secret) {
        this.secret = secret;
    }

    public String createGatewayTransaction() { return "GW-" + UUID.randomUUID(); }

    public void dispatch(UUID paymentId, String reference, BigDecimal amount, String currency, GatewayMode mode,
                         Consumer<GatewayCallback> callbackConsumer) {
        switch (mode) {
            case SUCCESS -> schedule(paymentId, reference, amount, currency, true, 100, callbackConsumer);
            case FAILURE -> schedule(paymentId, reference, amount, currency, false, 100, callbackConsumer);
            case DELAYED_SUCCESS -> schedule(paymentId, reference, amount, currency, true, 3500, callbackConsumer);
            case DUPLICATE_SUCCESS -> {
                schedule(paymentId, reference, amount, currency, true, 100, callbackConsumer);
                schedule(paymentId, reference, amount, currency, true, 250, callbackConsumer);
            }
            case INVALID_SIGNATURE -> scheduler.schedule(
                    () -> callbackConsumer.accept(new GatewayCallback(paymentId, reference, amount, currency, true, "invalid-signature")),
                    100, TimeUnit.MILLISECONDS);
            case TIMEOUT -> { /* deliberately no callback */ }
        }
    }

    private void schedule(UUID paymentId, String reference, BigDecimal amount, String currency, boolean success, long delayMs,
                          Consumer<GatewayCallback> callbackConsumer) {
        scheduler.schedule(() -> callbackConsumer.accept(
                new GatewayCallback(paymentId, reference, amount, currency, success, sign(paymentId, reference, amount, currency, success))),
                delayMs, TimeUnit.MILLISECONDS);
    }

    public String sign(UUID paymentId, String gatewayReference, BigDecimal amount, String currency, boolean success) {
        try {
            String payload = paymentId + ":" + gatewayReference + ":" + amount.toPlainString() + ":" + currency + ":" + success;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("unable to sign callback", e);
        }
    }

    public boolean verify(UUID paymentId, String gatewayReference, BigDecimal amount, String currency, boolean success, String signature) {
        byte[] expected = sign(paymentId, gatewayReference, amount, currency, success).getBytes(StandardCharsets.UTF_8);
        byte[] actual = signature == null ? new byte[0] : signature.toLowerCase().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    public record GatewayCallback(UUID paymentId, String gatewayReference, BigDecimal amount, String currency, boolean success, String signature) {}
}
