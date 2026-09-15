package com.ihya.api.catalogue;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * HTTP surface for the Sunnah half of the catalogue.
 *
 * <p>Reads ({@code GET}) are open to any authenticated user. {@code GET /sunnahs}
 * returns the full catalogue as one list, in the curated default order — per
 * docs/api-contract.md §0 "the catalogue is returned as a full list (small,
 * long-cached)" — not paginated. Writes are gated with
 * {@code @PreAuthorize("hasRole('ADMIN')")}; see {@link CategoryController} for
 * how that is enforced.
 */
@RestController
@RequestMapping("/sunnahs")
public class SunnahController {

    private final SunnahService sunnahService;

    public SunnahController(SunnahService sunnahService) {
        this.sunnahService = sunnahService;
    }

    @GetMapping
    public List<SunnahResponse> getAll() {
        return sunnahService.getAll().stream()
                .map(SunnahResponse::from)
                .toList();
    }

    @GetMapping("/{slug}")
    public SunnahResponse getBySlug(@PathVariable String slug) {
        return SunnahResponse.from(sunnahService.getBySlug(slug));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public SunnahResponse create(@Valid @RequestBody SunnahCreateRequest request) {
        return SunnahResponse.from(sunnahService.create(request));
    }

    @PatchMapping("/{slug}")
    @PreAuthorize("hasRole('ADMIN')")
    public SunnahResponse update(@PathVariable String slug, @RequestBody SunnahUpdateRequest request) {
        return SunnahResponse.from(sunnahService.update(slug, request));
    }

    @DeleteMapping("/{slug}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable String slug) {
        sunnahService.delete(slug);
    }
}
