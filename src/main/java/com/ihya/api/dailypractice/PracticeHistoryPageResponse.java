package com.ihya.api.dailypractice;

import java.util.List;

/** Client-facing shape for {@code GET /practices} (docs/api-contract.md §0 "Pagination"). */
public record PracticeHistoryPageResponse(List<PracticeHistoryItemResponse> items, String nextCursor) {
}
