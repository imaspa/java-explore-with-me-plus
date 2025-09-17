package ru.practicum.ewm.service.category;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ru.practicum.ewm.core.exception.NotFoundException;
import ru.practicum.ewm.core.exception.ValidateException;
import ru.practicum.ewm.dto.category.CategoryDto;
import ru.practicum.ewm.dto.category.NewCategoryDto;
import ru.practicum.ewm.mapper.CategoryMapper;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.repository.CategoryRepository;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@Validated
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository repository;
    private final CategoryMapper mapper;

    @Override
    @Transactional
    public CategoryDto create(NewCategoryDto newCategoryDto) {
        checkCategoryOrThrow(newCategoryDto.getName());
        Category category = repository.save(mapper.toEntity(newCategoryDto));
        log.info("Создана категория {}, id = {}", category.getName(), category.getId());
        return mapper.toDto(category);
    }

    @Override
    @Transactional
    public CategoryDto update(Long catId, NewCategoryDto newCategoryDto) {
        checkCategoryOrThrow(newCategoryDto.getName());
        Category category = repository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id = " + catId + " не найдена"));
        category.setName(newCategoryDto.getName());
        category = repository.save(category);
        log.info("Изменение категории OK");
        return mapper.toDto(category);
    }

    @Override
    @Transactional
    public void delete(Long catId) {
        //TODO с категорией не должно быть связано ни одного события (ValidateException 409)
        Category category = repository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id = " + catId + " не найдена"));
        repository.delete(category);
        log.info("Категория {} удалена", catId);
    }

    @Override
    public List<CategoryDto> findCategories(Pageable pageable) {
        return repository.findAll().stream()
                .map(mapper::toDto).toList();
    }

    @Override
    public CategoryDto findById(Long catId) {
        Category category = repository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id = " + catId + " не найдена"));
        return mapper.toDto(category);
    }

    private void checkCategoryOrThrow(String name) {
        log.info("Проверка неповторяемости названия категории");
        if (repository.findAll().stream()
                .anyMatch(category -> category.getName().equals(name))) {
            throw new ValidateException("Категория " + name + " уже существует");
        }
    }
}
