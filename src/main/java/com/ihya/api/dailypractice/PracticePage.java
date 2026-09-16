package com.ihya.api.dailypractice;

import java.util.List;

/**
 * One cursor-paginated page of a user's practice history
 * (docs/api-contract.md §0 "Pagination"). {@code nextCursor} is {@code null}
 * at the end of the history.
 */
public record PracticePage(List<Practice> items, String nextCursor) {
}
