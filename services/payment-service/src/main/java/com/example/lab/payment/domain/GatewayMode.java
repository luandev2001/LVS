package com.example.lab.payment.domain;

public enum GatewayMode {
    SUCCESS, FAILURE, TIMEOUT, DELAYED_SUCCESS, DUPLICATE_SUCCESS, INVALID_SIGNATURE
}
