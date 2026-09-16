package com.ihya.api.dailypractice;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Incoming payload for {@code POST /practices}. {@code feeling} is optional. */
public record RecordPracticeRequest(@NotNull UUID sunnahId, String feeling) {
}
