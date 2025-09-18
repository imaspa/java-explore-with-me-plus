package ru.practicum.ewm.mapper;

import org.mapstruct.Mapper;
import ru.practicum.ewm.core.config.CommonMapperConfiguration;
import ru.practicum.ewm.dto.category.CategoryDto;
import ru.practicum.ewm.dto.category.NewCategoryDto;
import ru.practicum.ewm.model.Category;

@Mapper(config = CommonMapperConfiguration.class)
public interface CategoryMapper {
    Category toEntity(NewCategoryDto dto);

    CategoryDto toDto(Category entity);
}
