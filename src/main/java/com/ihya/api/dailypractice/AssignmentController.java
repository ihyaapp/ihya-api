package com.ihya.api.dailypractice;

import com.ihya.api.catalogue.Sunnah;
import com.ihya.api.catalogue.SunnahResponse;
import com.ihya.api.catalogue.SunnahService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * The user's assigned Sunnah for their current local day
 * (docs/api-contract.md §2 "Daily practice").
 */
@RestController
@RequestMapping("/assignment")
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final SunnahService sunnahService;

    public AssignmentController(AssignmentService assignmentService, SunnahService sunnahService) {
        this.assignmentService = assignmentService;
        this.sunnahService = sunnahService;
    }

    @GetMapping("/today")
    public AssignmentResponse getToday(@AuthenticationPrincipal UUID userId) {
        AssignmentResult result = assignmentService.getTodayAssignment(userId);
        Sunnah sunnah = sunnahService.getById(result.assignment().getSunnahId());
        return new AssignmentResponse(SunnahResponse.from(sunnah), result.replacementAvailable());
    }

    @PostMapping("/replacement")
    public ReplacementResponse requestReplacement(@AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ReplacementRequest request) {
        AssignmentResult result = assignmentService.requestReplacement(userId, request.reason());
        Sunnah sunnah = sunnahService.getById(result.assignment().getSunnahId());
        return new ReplacementResponse(SunnahResponse.from(sunnah));
    }
}
