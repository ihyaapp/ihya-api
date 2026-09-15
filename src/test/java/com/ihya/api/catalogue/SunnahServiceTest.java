package com.ihya.api.catalogue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link SunnahService}: both repositories are Mockito mocks,
 * the service is constructed by hand, no Spring context and no database. Style
 * matches {@code UserServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
class SunnahServiceTest {

    @Mock
    private SunnahRepository sunnahRepository;
    @Mock
    private CategoryRepository categoryRepository;

    private SunnahService sunnahService;

    @BeforeEach
    void setUp() {
        sunnahService = new SunnahService(sunnahRepository, categoryRepository);
    }

    // ------------------------------------------------------------------
    // create()
    // ------------------------------------------------------------------

    @Test
    void create_validInput_trimsTextAndPersistsSunnahUnderCategory() {
        Category category = new Category("health-cleanliness", "Health & Cleanliness", null, "active", 0);
        when(categoryRepository.findBySlug("health-cleanliness")).thenReturn(Optional.of(category));
        when(sunnahRepository.saveAndFlush(any(Sunnah.class))).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<Sunnah> captor = ArgumentCaptor.forClass(Sunnah.class);

        sunnahService.create(new SunnahCreateRequest(
                "  use-the-miswak  ", "  Use the miswak  ", "health-cleanliness", "  Bukhari 887  ",
                "  Cleans the mouth  ", "  Brush before wudu  ", "  arabic  ", "  prompt?  ",
                List.of("hygiene")));

        verify(sunnahRepository).saveAndFlush(captor.capture());
        Sunnah saved = captor.getValue();
        assertThat(saved.getSlug()).isEqualTo("use-the-miswak");
        assertThat(saved.getTitle()).isEqualTo("Use the miswak");
        assertThat(saved.getDescription()).isEqualTo("Cleans the mouth");
        assertThat(saved.getReflection()).isEqualTo("Brush before wudu");
        assertThat(saved.getSource()).isEqualTo("Bukhari 887");
        assertThat(saved.getArabicText()).isEqualTo("arabic");
        assertThat(saved.getPrompt()).isEqualTo("prompt?");
        assertThat(saved.getTags()).containsExactly("hygiene");
        assertThat(saved.getCategory()).isSameAs(category);
    }

    @Test
    void create_blankArabicTextAndPrompt_areStoredAsNull() {
        when(categoryRepository.findBySlug("health-cleanliness"))
                .thenReturn(Optional.of(new Category("health-cleanliness", "Health & Cleanliness", null, "active", 0)));
        when(sunnahRepository.saveAndFlush(any(Sunnah.class))).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<Sunnah> captor = ArgumentCaptor.forClass(Sunnah.class);

        sunnahService.create(new SunnahCreateRequest("slug", "Title", "health-cleanliness", "Source",
                "Description", "Reflection", "   ", "   ", null));

        verify(sunnahRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getArabicText()).isNull();
        assertThat(captor.getValue().getPrompt()).isNull();
        assertThat(captor.getValue().getTags()).isEmpty();
    }

    @Test
    void create_blankTitle_throwsIllegalArgumentExceptionAndDoesNotPersist() {
        Throwable thrown = catchThrowable(() -> sunnahService.create(new SunnahCreateRequest(
                "slug", "  ", "health-cleanliness", "Source", "Description", "Reflection", null, null, null)));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title must not be blank");
        verifyNoInteractions(categoryRepository, sunnahRepository);
    }

    @Test
    void create_blankDescription_throwsIllegalArgumentExceptionAndDoesNotPersist() {
        Throwable thrown = catchThrowable(() -> sunnahService.create(new SunnahCreateRequest(
                "slug", "Title", "health-cleanliness", "Source", "   ", "Reflection", null, null, null)));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("description must not be blank");
        verifyNoInteractions(categoryRepository, sunnahRepository);
    }

    @Test
    void create_blankReflection_throwsIllegalArgumentExceptionAndDoesNotPersist() {
        Throwable thrown = catchThrowable(() -> sunnahService.create(new SunnahCreateRequest(
                "slug", "Title", "health-cleanliness", "Source", "Description", "", null, null, null)));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reflection must not be blank");
        verifyNoInteractions(categoryRepository, sunnahRepository);
    }

    @Test
    void create_blankSource_throwsIllegalArgumentExceptionAndDoesNotPersist() {
        Throwable thrown = catchThrowable(() -> sunnahService.create(new SunnahCreateRequest(
                "slug", "Title", "health-cleanliness", "  ", "Description", "Reflection", null, null, null)));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("source must not be blank");
        verifyNoInteractions(categoryRepository, sunnahRepository);
    }

    @Test
    void create_nonexistentCategorySlug_throwsCategoryNotFoundExceptionAndDoesNotPersist() {
        when(categoryRepository.findBySlug("ghost")).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> sunnahService.create(new SunnahCreateRequest(
                "slug", "Title", "ghost", "Source", "Description", "Reflection", null, null, null)));

        assertThat(thrown)
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessageContaining("ghost");
        verify(sunnahRepository, never()).saveAndFlush(any());
    }

    @Test
    void create_duplicateSlug_throwsSunnahSlugAlreadyExistsException() {
        when(categoryRepository.findBySlug("health-cleanliness"))
                .thenReturn(Optional.of(new Category("health-cleanliness", "Health & Cleanliness", null, "active", 0)));
        when(sunnahRepository.saveAndFlush(any(Sunnah.class))).thenThrow(new DataIntegrityViolationException(
                "could not execute statement [ERROR: duplicate key value violates unique "
                        + "constraint \"sunnahs_slug_key\"]"));

        Throwable thrown = catchThrowable(() -> sunnahService.create(new SunnahCreateRequest(
                "use-the-miswak", "Title", "health-cleanliness", "Source", "Description", "Reflection",
                null, null, null)));

        assertThat(thrown)
                .isInstanceOf(SunnahSlugAlreadyExistsException.class)
                .hasMessageContaining("use-the-miswak");
    }

    // ------------------------------------------------------------------
    // getById() / getBySlug()
    // ------------------------------------------------------------------

    @Test
    void getById_existingId_returnsSunnah() {
        UUID id = UUID.randomUUID();
        Sunnah sunnah = sunnahWithId(id, "use-the-miswak");
        when(sunnahRepository.findById(id)).thenReturn(Optional.of(sunnah));

        Sunnah result = sunnahService.getById(id);

        assertThat(result).isSameAs(sunnah);
    }

    @Test
    void getById_unknownId_throwsSunnahNotFoundException() {
        UUID id = UUID.randomUUID();
        when(sunnahRepository.findById(id)).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> sunnahService.getById(id));

        assertThat(thrown)
                .isInstanceOf(SunnahNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void getBySlug_existingSlug_returnsSunnah() {
        Sunnah sunnah = sunnahWithId(UUID.randomUUID(), "use-the-miswak");
        when(sunnahRepository.findBySlug("use-the-miswak")).thenReturn(Optional.of(sunnah));

        Sunnah result = sunnahService.getBySlug("use-the-miswak");

        assertThat(result).isSameAs(sunnah);
    }

    @Test
    void getBySlug_unknownSlug_throwsSunnahNotFoundException() {
        when(sunnahRepository.findBySlug("ghost")).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> sunnahService.getBySlug("ghost"));

        assertThat(thrown)
                .isInstanceOf(SunnahNotFoundException.class)
                .hasMessageContaining("ghost");
    }

    // ------------------------------------------------------------------
    // getAll() / countByCategory()
    // ------------------------------------------------------------------

    @Test
    void getAll_returnsCuratedOrderFromRepository() {
        List<Sunnah> sunnahs = List.of(sunnahWithId(UUID.randomUUID(), "a"), sunnahWithId(UUID.randomUUID(), "b"));
        when(sunnahRepository.findAllOrderedByCategoryThenCreatedAt()).thenReturn(sunnahs);

        List<Sunnah> result = sunnahService.getAll();

        assertThat(result).isEqualTo(sunnahs);
    }

    @Test
    void countByCategory_delegatesToRepository() {
        UUID categoryId = UUID.randomUUID();
        when(sunnahRepository.countByCategoryId(categoryId)).thenReturn(3L);

        long result = sunnahService.countByCategory(categoryId);

        assertThat(result).isEqualTo(3L);
    }

    // ------------------------------------------------------------------
    // update()
    // ------------------------------------------------------------------

    @Test
    void update_validInput_appliesTrimmedChangesReassignsCategoryAndPersists() {
        Sunnah existing = sunnahWithId(UUID.randomUUID(), "use-the-miswak");
        Category newCategory = new Category("fasting", "Fasting", null, "active", 0);
        when(sunnahRepository.findBySlug("use-the-miswak")).thenReturn(Optional.of(existing));
        when(categoryRepository.findBySlug("fasting")).thenReturn(Optional.of(newCategory));
        when(sunnahRepository.saveAndFlush(any(Sunnah.class))).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<Sunnah> captor = ArgumentCaptor.forClass(Sunnah.class);

        Sunnah result = sunnahService.update("use-the-miswak", new SunnahUpdateRequest(
                "  New Title  ", "fasting", "  Muslim 1  ", "  New Desc  ", "  New Reflection  ",
                null, null, List.of("ramadan")));

        verify(sunnahRepository).saveAndFlush(captor.capture());
        Sunnah saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("New Title");
        assertThat(saved.getDescription()).isEqualTo("New Desc");
        assertThat(saved.getReflection()).isEqualTo("New Reflection");
        assertThat(saved.getSource()).isEqualTo("Muslim 1");
        assertThat(saved.getTags()).containsExactly("ramadan");
        assertThat(saved.getCategory()).isSameAs(newCategory);
        assertThat(result).isSameAs(existing);
    }

    @Test
    void update_partialRequest_keepsOmittedFieldsUnchanged() {
        Sunnah existing = sunnahWithId(UUID.randomUUID(), "use-the-miswak");
        when(sunnahRepository.findBySlug("use-the-miswak")).thenReturn(Optional.of(existing));
        when(sunnahRepository.saveAndFlush(any(Sunnah.class))).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<Sunnah> captor = ArgumentCaptor.forClass(Sunnah.class);

        sunnahService.update("use-the-miswak", new SunnahUpdateRequest(
                null, null, null, "New Desc only", null, null, null, null));

        verify(sunnahRepository).saveAndFlush(captor.capture());
        Sunnah saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo(existing.getTitle());
        assertThat(saved.getDescription()).isEqualTo("New Desc only");
        assertThat(saved.getReflection()).isEqualTo(existing.getReflection());
        assertThat(saved.getSource()).isEqualTo(existing.getSource());
        assertThat(saved.getCategory()).isSameAs(existing.getCategory());
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void update_unknownSunnahSlug_throwsSunnahNotFoundExceptionAndDoesNotPersist() {
        when(sunnahRepository.findBySlug("ghost")).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> sunnahService.update("ghost", new SunnahUpdateRequest(
                "Title", null, "Source", "Desc", "Reflection", null, null, null)));

        assertThat(thrown)
                .isInstanceOf(SunnahNotFoundException.class)
                .hasMessageContaining("ghost");
        verify(sunnahRepository, never()).saveAndFlush(any());
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void update_nonexistentCategorySlug_throwsCategoryNotFoundExceptionAndDoesNotPersist() {
        Sunnah existing = sunnahWithId(UUID.randomUUID(), "use-the-miswak");
        when(sunnahRepository.findBySlug("use-the-miswak")).thenReturn(Optional.of(existing));
        when(categoryRepository.findBySlug("ghost")).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> sunnahService.update("use-the-miswak", new SunnahUpdateRequest(
                "Title", "ghost", "Source", "Desc", "Reflection", null, null, null)));

        assertThat(thrown)
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessageContaining("ghost");
        verify(sunnahRepository, never()).saveAndFlush(any());
    }

    @Test
    void update_blankTitle_throwsIllegalArgumentExceptionAndDoesNotPersist() {
        Sunnah existing = sunnahWithId(UUID.randomUUID(), "use-the-miswak");
        when(sunnahRepository.findBySlug("use-the-miswak")).thenReturn(Optional.of(existing));

        Throwable thrown = catchThrowable(() -> sunnahService.update("use-the-miswak", new SunnahUpdateRequest(
                "  ", null, null, null, null, null, null, null)));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title must not be blank");
        verify(sunnahRepository, never()).saveAndFlush(any());
    }

    // ------------------------------------------------------------------
    // delete()
    // ------------------------------------------------------------------

    @Test
    void delete_existingSlug_deletesEntity() {
        Sunnah sunnah = sunnahWithId(UUID.randomUUID(), "use-the-miswak");
        when(sunnahRepository.findBySlug("use-the-miswak")).thenReturn(Optional.of(sunnah));

        sunnahService.delete("use-the-miswak");

        verify(sunnahRepository).delete(sunnah);
    }

    @Test
    void delete_unknownSlug_throwsSunnahNotFoundExceptionAndDoesNotDelete() {
        when(sunnahRepository.findBySlug("ghost")).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> sunnahService.delete("ghost"));

        assertThat(thrown).isInstanceOf(SunnahNotFoundException.class);
        verify(sunnahRepository, never()).delete(any());
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    /** Builds a {@link Sunnah} with its generated {@code id} set reflectively. */
    private static Sunnah sunnahWithId(UUID id, String slug) {
        Sunnah sunnah = new Sunnah(new Category("health-cleanliness", "Health & Cleanliness", null, "active", 0),
                slug, "Title", "Description", "Reflection", "Source", null, null, List.of());
        try {
            Field idField = Sunnah.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(sunnah, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not set Sunnah.id for test fixture", e);
        }
        return sunnah;
    }
}
