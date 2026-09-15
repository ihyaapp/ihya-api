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
 * Pure unit tests for {@link CategoryService}: the repository is a Mockito mock,
 * the service is constructed by hand, no Spring context and no database. Style
 * matches {@code UserServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryRepository);
    }

    // ------------------------------------------------------------------
    // create()
    // ------------------------------------------------------------------

    @Test
    void create_validInput_trimsFieldsAndPersistsCategory() {
        Category persisted = categoryWithId(UUID.randomUUID(), "faith-worship", "Faith & Worship", "Salah practices",
                "active", 0);
        when(categoryRepository.saveAndFlush(any(Category.class))).thenReturn(persisted);
        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);

        Category result = categoryService.create(
                new CategoryCreateRequest("  faith-worship  ", "  Faith & Worship  ", "Salah practices", "active"));

        verify(categoryRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getSlug()).isEqualTo("faith-worship");
        assertThat(captor.getValue().getName()).isEqualTo("Faith & Worship");
        assertThat(captor.getValue().getDescription()).isEqualTo("Salah practices");
        assertThat(captor.getValue().getStatus()).isEqualTo("active");
        assertThat(result).isSameAs(persisted);
    }

    @Test
    void create_statusOmitted_defaultsToActive() {
        when(categoryRepository.saveAndFlush(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);

        categoryService.create(new CategoryCreateRequest("faith-worship", "Faith & Worship", null, null));

        verify(categoryRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("active");
    }

    @Test
    void create_comingSoonStatus_isAccepted() {
        when(categoryRepository.saveAndFlush(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);

        categoryService.create(new CategoryCreateRequest("self-care", "Self Care", null, "coming-soon"));

        verify(categoryRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("coming-soon");
    }

    @Test
    void create_invalidStatus_throwsIllegalArgumentExceptionAndDoesNotPersist() {
        Throwable thrown = catchThrowable(() ->
                categoryService.create(new CategoryCreateRequest("faith-worship", "Faith & Worship", null, "retired")));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("status must be one of");
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void create_blankSlug_throwsIllegalArgumentExceptionAndDoesNotPersist() {
        Throwable thrown = catchThrowable(() ->
                categoryService.create(new CategoryCreateRequest("   ", "Faith & Worship", null, "active")));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("slug must not be blank");
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void create_blankName_throwsIllegalArgumentExceptionAndDoesNotPersist() {
        Throwable thrown = catchThrowable(() ->
                categoryService.create(new CategoryCreateRequest("faith-worship", "   ", null, "active")));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name must not be blank");
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void create_duplicateName_throwsCategoryNameAlreadyExistsException() {
        when(categoryRepository.saveAndFlush(any(Category.class))).thenThrow(new DataIntegrityViolationException(
                "could not execute statement [ERROR: duplicate key value violates unique "
                        + "constraint \"categories_name_key\"]"));

        Throwable thrown = catchThrowable(() ->
                categoryService.create(new CategoryCreateRequest("faith-worship", "Faith & Worship", null, "active")));

        assertThat(thrown)
                .isInstanceOf(CategoryNameAlreadyExistsException.class)
                .hasMessageContaining("Faith & Worship");
    }

    @Test
    void create_duplicateSlug_throwsCategorySlugAlreadyExistsException() {
        when(categoryRepository.saveAndFlush(any(Category.class))).thenThrow(new DataIntegrityViolationException(
                "could not execute statement [ERROR: duplicate key value violates unique "
                        + "constraint \"categories_slug_key\"]"));

        Throwable thrown = catchThrowable(() ->
                categoryService.create(new CategoryCreateRequest("faith-worship", "Faith & Worship", null, "active")));

        assertThat(thrown)
                .isInstanceOf(CategorySlugAlreadyExistsException.class)
                .hasMessageContaining("faith-worship");
    }

    @Test
    void create_unrelatedIntegrityViolation_propagatesUnchanged() {
        DataIntegrityViolationException dbError = new DataIntegrityViolationException(
                "could not execute statement [ERROR: null value in column \"created_at\" violates not-null constraint]");
        when(categoryRepository.saveAndFlush(any(Category.class))).thenThrow(dbError);

        Throwable thrown = catchThrowable(() ->
                categoryService.create(new CategoryCreateRequest("faith-worship", "Faith & Worship", null, "active")));

        assertThat(thrown).isSameAs(dbError);
    }

    // ------------------------------------------------------------------
    // getBySlug()
    // ------------------------------------------------------------------

    @Test
    void getBySlug_existingSlug_returnsCategory() {
        Category category = categoryWithId(UUID.randomUUID(), "fasting", "Fasting", null, "active", 0);
        when(categoryRepository.findBySlug("fasting")).thenReturn(Optional.of(category));

        Category result = categoryService.getBySlug("fasting");

        assertThat(result).isSameAs(category);
    }

    @Test
    void getBySlug_unknownSlug_throwsCategoryNotFoundException() {
        when(categoryRepository.findBySlug("ghost")).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> categoryService.getBySlug("ghost"));

        assertThat(thrown)
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessageContaining("ghost");
    }

    // ------------------------------------------------------------------
    // update()
    // ------------------------------------------------------------------

    @Test
    void update_validInput_appliesTrimmedChangesAndPersists() {
        Category existing = categoryWithId(UUID.randomUUID(), "faith-worship", "Prayer", "old description",
                "active", 0);
        when(categoryRepository.findBySlug("faith-worship")).thenReturn(Optional.of(existing));
        when(categoryRepository.saveAndFlush(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);

        Category result = categoryService.update("faith-worship",
                new CategoryUpdateRequest("Faith & Worship", "new description", "coming-soon"));

        verify(categoryRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Faith & Worship");
        assertThat(captor.getValue().getDescription()).isEqualTo("new description");
        assertThat(captor.getValue().getStatus()).isEqualTo("coming-soon");
        assertThat(result).isSameAs(existing);
    }

    @Test
    void update_partialRequest_keepsOmittedFieldsUnchanged() {
        Category existing = categoryWithId(UUID.randomUUID(), "faith-worship", "Prayer", "old description",
                "active", 0);
        when(categoryRepository.findBySlug("faith-worship")).thenReturn(Optional.of(existing));
        when(categoryRepository.saveAndFlush(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);

        categoryService.update("faith-worship", new CategoryUpdateRequest(null, "new description", null));

        verify(categoryRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Prayer");
        assertThat(captor.getValue().getDescription()).isEqualTo("new description");
        assertThat(captor.getValue().getStatus()).isEqualTo("active");
    }

    @Test
    void update_unknownSlug_throwsCategoryNotFoundExceptionAndDoesNotPersist() {
        when(categoryRepository.findBySlug("ghost")).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() ->
                categoryService.update("ghost", new CategoryUpdateRequest("Salah", "desc", "active")));

        assertThat(thrown)
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessageContaining("ghost");
        verify(categoryRepository, never()).saveAndFlush(any());
    }

    @Test
    void update_blankName_throwsIllegalArgumentExceptionAndDoesNotPersist() {
        when(categoryRepository.findBySlug("faith-worship"))
                .thenReturn(Optional.of(categoryWithId(UUID.randomUUID(), "faith-worship", "Prayer", null,
                        "active", 0)));

        Throwable thrown = catchThrowable(() ->
                categoryService.update("faith-worship", new CategoryUpdateRequest("   ", "desc", "active")));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name must not be blank");
        verify(categoryRepository, never()).saveAndFlush(any());
    }

    @Test
    void update_invalidStatus_throwsIllegalArgumentExceptionAndDoesNotPersist() {
        when(categoryRepository.findBySlug("faith-worship"))
                .thenReturn(Optional.of(categoryWithId(UUID.randomUUID(), "faith-worship", "Prayer", null,
                        "active", 0)));

        Throwable thrown = catchThrowable(() ->
                categoryService.update("faith-worship", new CategoryUpdateRequest(null, null, "retired")));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("status must be one of");
        verify(categoryRepository, never()).saveAndFlush(any());
    }

    @Test
    void update_renameToExistingName_throwsCategoryNameAlreadyExistsException() {
        when(categoryRepository.findBySlug("faith-worship"))
                .thenReturn(Optional.of(categoryWithId(UUID.randomUUID(), "faith-worship", "Prayer", null,
                        "active", 0)));
        when(categoryRepository.saveAndFlush(any(Category.class))).thenThrow(new DataIntegrityViolationException(
                "could not execute statement [ERROR: duplicate key value violates unique "
                        + "constraint \"categories_name_key\"]"));

        Throwable thrown = catchThrowable(() ->
                categoryService.update("faith-worship", new CategoryUpdateRequest("Fasting", "desc", "active")));

        assertThat(thrown)
                .isInstanceOf(CategoryNameAlreadyExistsException.class)
                .hasMessageContaining("Fasting");
    }

    @Test
    void update_unrelatedIntegrityViolation_propagatesUnchanged() {
        DataIntegrityViolationException dbError = new DataIntegrityViolationException(
                "could not execute statement [ERROR: some other constraint]");
        when(categoryRepository.findBySlug("faith-worship"))
                .thenReturn(Optional.of(categoryWithId(UUID.randomUUID(), "faith-worship", "Prayer", null,
                        "active", 0)));
        when(categoryRepository.saveAndFlush(any(Category.class))).thenThrow(dbError);

        Throwable thrown = catchThrowable(() ->
                categoryService.update("faith-worship", new CategoryUpdateRequest("Salah", "desc", "active")));

        assertThat(thrown).isSameAs(dbError);
    }

    // ------------------------------------------------------------------
    // getAll()
    // ------------------------------------------------------------------

    @Test
    void getAll_returnsRepositoryContents() {
        List<Category> categories = List.of(
                categoryWithId(UUID.randomUUID(), "faith-worship", "Prayer", null, "active", 0),
                categoryWithId(UUID.randomUUID(), "fasting", "Fasting", null, "active", 1));
        when(categoryRepository.findAll()).thenReturn(categories);

        List<Category> result = categoryService.getAll();

        assertThat(result).isEqualTo(categories);
    }

    // ------------------------------------------------------------------
    // delete()
    // ------------------------------------------------------------------

    @Test
    void delete_existingSlug_deletesEntity() {
        Category category = categoryWithId(UUID.randomUUID(), "faith-worship", "Prayer", null, "active", 0);
        when(categoryRepository.findBySlug("faith-worship")).thenReturn(Optional.of(category));

        categoryService.delete("faith-worship");

        verify(categoryRepository).delete(category);
    }

    @Test
    void delete_unknownSlug_throwsCategoryNotFoundExceptionAndDoesNotDelete() {
        when(categoryRepository.findBySlug("ghost")).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> categoryService.delete("ghost"));

        assertThat(thrown).isInstanceOf(CategoryNotFoundException.class);
        verify(categoryRepository, never()).delete(any());
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    /**
     * Builds a {@link Category} with its generated {@code id} populated.
     * {@code Category} has no id setter (Hibernate assigns it on persist), so
     * tests simulate a saved row by setting the field reflectively — same
     * approach as {@code UserServiceTest}.
     */
    private static Category categoryWithId(UUID id, String slug, String name, String description, String status,
                                            int sortOrder) {
        Category category = new Category(slug, name, description, status, sortOrder);
        try {
            Field idField = Category.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(category, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not set Category.id for test fixture", e);
        }
        return category;
    }
}
