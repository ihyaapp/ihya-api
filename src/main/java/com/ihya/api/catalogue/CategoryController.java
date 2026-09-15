package com.ihya.api.catalogue;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * HTTP surface for the category half of the catalogue.
 *
 * <p>Reads ({@code GET}) are open to any authenticated user — {@code SecurityConfig}
 * already requires authentication on every non-{@code /auth} route, so nothing
 * extra is needed here for those. Writes are additionally gated with
 * {@code @PreAuthorize("hasRole('ADMIN')")}, enforced by
 * {@code @EnableMethodSecurity} in {@code SecurityConfig}, checking the
 * {@code ROLE_ADMIN} authority {@link com.ihya.api.identity.JwtAuthenticationFilter}
 * attaches. There is no {@code DELETE /categories}: only Sunnahs are deletable
 * per docs/api-contract.md §2.
 *
 * <p>{@code sunnahCount} on each {@link CategoryResponse} is derived by asking
 * {@link SunnahService} rather than stored on the entity — see
 * ihya-mobile/docs/content-model.md ("count is now derived").
 */
@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final SunnahService sunnahService;

    public CategoryController(CategoryService categoryService, SunnahService sunnahService) {
        this.categoryService = categoryService;
        this.sunnahService = sunnahService;
    }

    @GetMapping
    public List<CategoryResponse> getAll() {
        return categoryService.getAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse create(@Valid @RequestBody CategoryCreateRequest request) {
        return toResponse(categoryService.create(request));
    }

    @PatchMapping("/{slug}")
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse update(@PathVariable String slug, @RequestBody CategoryUpdateRequest request) {
        return toResponse(categoryService.update(slug, request));
    }

    private CategoryResponse toResponse(Category category) {
        long sunnahCount = sunnahService.countByCategory(category.getId());
        return CategoryResponse.from(category, sunnahCount);
    }
}
