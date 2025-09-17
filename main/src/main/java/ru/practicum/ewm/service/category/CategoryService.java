package ru.practicum.ewm.service.category;

import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.dto.category.CategoryDto;
import ru.practicum.ewm.dto.category.NewCategoryDto;

import java.util.List;

public interface CategoryService {
    CategoryDto create(NewCategoryDto newCategoryDto);

    CategoryDto update(Long catId, NewCategoryDto newCategoryDto);
    void delete(Long categoryId);

    List<CategoryDto> findCategories(Pageable pageable);

    CategoryDto findById(Long categoryId);

}
