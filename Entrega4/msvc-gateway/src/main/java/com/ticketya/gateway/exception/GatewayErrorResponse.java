package com.ticketya.gateway.exception;

public record GatewayErrorResponse(
        int status,
        String error,
        String message,
        String path
) {
}
