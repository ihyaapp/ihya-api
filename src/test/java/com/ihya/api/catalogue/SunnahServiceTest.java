package com.ihya.api.catalogue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
        UUID categoryId = UUID.randomUUID();
        Category category = new Category("Prayer", null);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(sunnahRepository.save(any(Sunnah.class))).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<Sunnah> captor = ArgumentCaptor.forClass(Sunnah.class);

        sunnahService.create(categoryId, "  Use the miswak  ", "  Cleans the mouth  ",
                "  Brush before wudu  ", "  Bukhari 887  ");

        verify(sunnahRepository).save(captor.capture());
        Sunnah saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("Use the miswak");
        assertThat(saved.getDescription()).isEqualTo("Cleans the mouth");
        assertThat(saved.getAction()).isEqualTo("Brush before wudu");
        assertThat(saved.getReference()).isEqualTo("Bukhari 887");
        assertThat(saved.getCategory()).isSameAs(category);
    }

    @Test
    void create_blankReference_isStoredAsNull() {
        UUID categoryId = UUID.randomUUID();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(new Category("Prayer", null)));
        when(sunnahRepository.save(any(Sunnah.class))).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<Sunnah> captor = ArgumentCaptor.forClass(Sunnah.class);

        sunnahService.create(categoryId, "Title", "Description", "Action", "   ");

        verify(sunnahRepository).save(captor.capture());
        assertThat(captor.getValue().getReference()).isNull();
    }

    @Test
    void create_blankTitle_throwsIllegalArgumentExceptionAndDoesNotPersist() {
        Throwable thrown = catchThrowable(() ->
                sunnahService.create(UUID.randomUUID(), "  ", "Description", "Action", null));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title must not be blank");
        verifyNoInteractions(categoryRepository, sunnahRepository);
    }

    @Test
    void create_blankDescription_throwsIllegalArgumentExceptionAndDoesNotPersist() {
        Throwable thrown = catchThrowable(() ->
                sunnahService.create(UUID.randomUUID(), "Title", "   ", "Action", null));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("description must not be blank");
        verifyNoInteractions(categoryRepository, sunnahRepository);
    }

    @Test
    void create_blankAction_throwsIllegalArgumentExceptionAndDoesNotPersist() {
        Throwable thrown = catchThrowable(() ->
                sunnahService.create(UUID.randomUUID(), "Title", "Description", "", null));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("action must not be blank");
        verifyNoInteractions(categoryRepository, sunnahRepository);
    }

    @Test
    void create_nonexistentCategoryId_throwsCategoryNotFoundExceptionAndDoesNotPersist() {
        UUID categoryId = UUID.randomUUID();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() ->
                sunnahService.create(categoryId, "Title", "Description", "Action", null));

        assertThat(thrown)
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessageContaining(categoryId.toString());
        verify(sunnahRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // getById()
    // ------------------------------------------------------------------

    @Test
    void getById_existingId_returnsSunnah() {
        UUID id = UUID.randomUUID();
        Sunnah sunnah = sunnahWithId(id);
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

    // ------------------------------------------------------------------
    // search()
    // ------------------------------------------------------------------

    @Test
    void search_trimsQueryBeforeDelegatingToRepository() {
        UUID categoryId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        Page<Sunnah> page = new PageImpl<>(List.of(sunnahWithId(UUID.randomUUID())));
        when(sunnahRepository.search("fasting", categoryId, pageable)).thenReturn(page);

        Page<Sunnah> result = sunnahService.search("  fasting  ", categoryId, pageable);

        verify(sunnahRepository).search("fasting", categoryId, pageable);
        assertThat(result).isSameAs(page);
    }

    @Test
    void search_blankQuery_delegatesWithNullQuery() {
        Pageable pageable = PageRequest.of(0, 20);
        when(sunnahRepository.search(null, null, pageable)).thenReturn(Page.empty());

        sunnahService.search("   ", null, pageable);

        verify(sunnahRepository).search(null, null, pageable);
    }

    @Test
    void search_nullQuery_delegatesWithNullQueryAndKeepsCategoryFilter() {
        UUID categoryId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        when(sunnahRepository.search(null, categoryId, pageable)).thenReturn(Page.empty());

        sunnahService.search(null, categoryId, pageable);

        verify(sunnahRepository).search(null, categoryId, pageable);
    }

    // ------------------------------------------------------------------
    // update()
    // ------------------------------------------------------------------

    @Test
    void update_validInput_appliesTrimmedChangesReassignsCategoryAndPersists() {
        UUID id = UUID.randomUUID();
        UUID newCategoryId = UUID.randomUUID();
        Sunnah existing = sunnahWithId(id);
        Category newCategory = new Category("Fasting", null);
        when(sunnahRepository.findById(id)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(newCategoryId)).thenReturn(Optional.of(newCategory));
        when(sunnahRepository.save(any(Sunnah.class))).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<Sunnah> captor = ArgumentCaptor.forClass(Sunnah.class);

        Sunnah result = sunnahService.update(id, "  New Title  ", "  New Desc  ",
                "  New Action  ", "  Muslim 1  ", newCategoryId);

        verify(sunnahRepository).save(captor.capture());
        Sunnah saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("New Title");
        assertThat(saved.getDescription()).isEqualTo("New Desc");
        assertThat(saved.getAction()).isEqualTo("New Action");
        assertThat(saved.getReference()).isEqualTo("Muslim 1");
        assertThat(saved.getCategory()).isSameAs(newCategory);
        assertThat(result).isSameAs(existing);
    }

    @Test
    void update_unknownSunnahId_throwsSunnahNotFoundExceptionAndDoesNotPersist() {
        UUID id = UUID.randomUUID();
        when(sunnahRepository.findById(id)).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() ->
                sunnahService.update(id, "Title", "Desc", "Action", null, UUID.randomUUID()));

        assertThat(thrown)
                .isInstanceOf(SunnahNotFoundException.class)
                .hasMessageContaining(id.toString());
        verify(sunnahRepository, never()).save(any());
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void update_nonexistentCategoryId_throwsCategoryNotFoundExceptionAndDoesNotPersist() {
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        when(sunnahRepository.findById(id)).thenReturn(Optional.of(sunnahWithId(id)));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() ->
                sunnahService.update(id, "Title", "Desc", "Action", null, categoryId));

        assertThat(thrown)
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessageContaining(categoryId.toString());
        verify(sunnahRepository, never()).save(any());
    }

    @Test
    void update_blankTitle_throwsIllegalArgumentExceptionAndDoesNotPersist() {
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        when(sunnahRepository.findById(id)).thenReturn(Optional.of(sunnahWithId(id)));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(new Category("Fasting", null)));

        Throwable thrown = catchThrowable(() ->
                sunnahService.update(id, "  ", "Desc", "Action", null, categoryId));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title must not be blank");
        verify(sunnahRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // delete()
    // ------------------------------------------------------------------

    @Test
    void delete_existingId_deletesEntity() {
        UUID id = UUID.randomUUID();
        Sunnah sunnah = sunnahWithId(id);
        when(sunnahRepository.findById(id)).thenReturn(Optional.of(sunnah));

        sunnahService.delete(id);

        verify(sunnahRepository).delete(sunnah);
    }

    @Test
    void delete_unknownId_throwsSunnahNotFoundExceptionAndDoesNotDelete() {
        UUID id = UUID.randomUUID();
        when(sunnahRepository.findById(id)).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> sunnahService.delete(id));

        assertThat(thrown).isInstanceOf(SunnahNotFoundException.class);
        verify(sunnahRepository, never()).delete(any());
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    /** Builds a {@link Sunnah} with its generated {@code id} set reflectively. */
    private static Sunnah sunnahWithId(UUID id) {
        Sunnah sunnah = new Sunnah(new Category("Prayer", null), "Title", "Description", "Action", null);
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
