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
    void create_validInput_trimsNameAndPersistsCategory() {
        Category persisted = categoryWithId(UUID.randomUUID(), "Prayer", "Salah practices");
        when(categoryRepository.saveAndFlush(any(Category.class))).thenReturn(persisted);
        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);

        Category result = categoryService.create("  Prayer  ", "Salah practices");

        verify(categoryRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Prayer");
        assertThat(captor.getValue().getDescription()).isEqualTo("Salah practices");
        assertThat(result).isSameAs(persisted);
    }

    @Test
    void create_blankName_throwsIllegalArgumentExceptionAndDoesNotPersist() {
        Throwable thrown = catchThrowable(() -> categoryService.create("   ", "some description"));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name must not be blank");
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void create_nullName_throwsIllegalArgumentException() {
        Throwable thrown = catchThrowable(() -> categoryService.create(null, "some description"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void create_duplicateName_throwsCategoryNameAlreadyExistsException() {
        when(categoryRepository.saveAndFlush(any(Category.class))).thenThrow(new DataIntegrityViolationException(
                "could not execute statement [ERROR: duplicate key value violates unique "
                        + "constraint \"categories_name_key\"]"));

        Throwable thrown = catchThrowable(() -> categoryService.create("Prayer", "dupe"));

        assertThat(thrown)
                .isInstanceOf(CategoryNameAlreadyExistsException.class)
                .hasMessageContaining("Prayer");
    }

    @Test
    void create_unrelatedIntegrityViolation_propagatesUnchanged() {
        DataIntegrityViolationException dbError = new DataIntegrityViolationException(
                "could not execute statement [ERROR: null value in column \"created_at\" violates not-null constraint]");
        when(categoryRepository.saveAndFlush(any(Category.class))).thenThrow(dbError);

        Throwable thrown = catchThrowable(() -> categoryService.create("Prayer", "desc"));

        assertThat(thrown).isSameAs(dbError);
    }

    // ------------------------------------------------------------------
    // getById()
    // ------------------------------------------------------------------

    @Test
    void getById_existingId_returnsCategory() {
        UUID id = UUID.randomUUID();
        Category category = categoryWithId(id, "Fasting", null);
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));

        Category result = categoryService.getById(id);

        assertThat(result).isSameAs(category);
    }

    @Test
    void getById_unknownId_throwsCategoryNotFoundException() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> categoryService.getById(id));

        assertThat(thrown)
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // ------------------------------------------------------------------
    // getAll()
    // ------------------------------------------------------------------

    @Test
    void getAll_returnsRepositoryContents() {
        List<Category> categories = List.of(
                categoryWithId(UUID.randomUUID(), "Prayer", null),
                categoryWithId(UUID.randomUUID(), "Fasting", null));
        when(categoryRepository.findAll()).thenReturn(categories);

        List<Category> result = categoryService.getAll();

        assertThat(result).isEqualTo(categories);
    }

    // ------------------------------------------------------------------
    // delete()
    // ------------------------------------------------------------------

    @Test
    void delete_existingId_deletesEntity() {
        UUID id = UUID.randomUUID();
        Category category = categoryWithId(id, "Prayer", null);
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));

        categoryService.delete(id);

        verify(categoryRepository).delete(category);
    }

    @Test
    void delete_unknownId_throwsCategoryNotFoundExceptionAndDoesNotDelete() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> categoryService.delete(id));

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
    private static Category categoryWithId(UUID id, String name, String description) {
        Category category = new Category(name, description);
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
