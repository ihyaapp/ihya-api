package com.ihya.api.dailypractice;

import com.ihya.api.catalogue.Sunnah;
import com.ihya.api.catalogue.SunnahResponse;
import com.ihya.api.catalogue.SunnahService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Recording practice completions and reporting progress
 * (docs/api-contract.md §2 "Daily practice" / "Streak").
 */
@RestController
public class PracticeController {

    private final PracticeService practiceService;
    private final SunnahService sunnahService;

    public PracticeController(PracticeService practiceService, SunnahService sunnahService) {
        this.practiceService = practiceService;
        this.sunnahService = sunnahService;
    }

    @PostMapping("/practices")
    public ResponseEntity<PracticeResultResponse> recordPractice(@AuthenticationPrincipal UUID userId,
            @Valid @RequestBody RecordPracticeRequest request) {
        PracticeRecordResult result = practiceService.recordPractice(userId, request.sunnahId(), request.feeling());
        HttpStatus status = result.alreadyExisted() ? HttpStatus.CONFLICT : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(PracticeResultResponse.from(result));
    }

    @PatchMapping("/practices/{id}")
    public PracticeResponse updateFeeling(@AuthenticationPrincipal UUID userId, @PathVariable UUID id,
            @Valid @RequestBody UpdateFeelingRequest request) {
        return PracticeResponse.from(practiceService.updateFeeling(userId, id, request.feeling()));
    }

    @GetMapping("/practices")
    public PracticeHistoryPageResponse listPractices(@AuthenticationPrincipal UUID userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        PracticePage page = practiceService.listPractices(userId, cursor, limit);

        // Full catalogue in one query rather than one lookup per item -- same
        // N+1-avoidance instinct as SunnahRepository's JOIN FETCH.
        Map<UUID, Sunnah> catalogueById = sunnahService.getAll().stream()
                .collect(Collectors.toMap(Sunnah::getId, Function.identity()));

        List<PracticeHistoryItemResponse> items = page.items().stream()
                .map(practice -> PracticeHistoryItemResponse.from(
                        practice, SunnahResponse.from(catalogueById.get(practice.getSunnahId()))))
                .toList();

        return new PracticeHistoryPageResponse(items, page.nextCursor());
    }

    @GetMapping("/me/progress")
    public ProgressResponse getProgress(@AuthenticationPrincipal UUID userId) {
        return ProgressResponse.from(practiceService.getProgress(userId));
    }
}
