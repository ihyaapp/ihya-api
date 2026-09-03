package com.ihya.api.identity;

public record AuthTokens(String accessToken, String refreshToken, long accessTokenExpiryMinutes) {
}