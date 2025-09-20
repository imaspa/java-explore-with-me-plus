package ru.practicum.ewm.service.category;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ru.practicum.ewm.core.exception.NotFoundException;
import ru.practicum.ewm.core.exception.ValidateException;
import ru.practicum.ewm.core.exception.WrongRequestException;
import ru.practicum.ewm.dto.category.CategoryDto;
import ru.practicum.ewm.dto.category.NewCategoryDto;
import ru.practicum.ewm.mapper.CategoryMapper;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.EventRepository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
@Validated
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository repository;
    private final CategoryMapper mapper;
    private final EventRepository eventRepository;

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
        Category category = repository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id = " + catId + " не найдена"));
        if (category.getName().equals(newCategoryDto.getName())) {
            log.info("Название категории не изменилось");
            return mapper.toDto(category);
        }
        checkCategoryOrThrow(newCategoryDto.getName());
        category.setName(newCategoryDto.getName());
        category = repository.save(category);
        log.info("Изменение категории OK");
        return mapper.toDto(category);
    }

    @Override
    @Transactional
    public void delete(Long catId) {
        Category category = repository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id = " + catId + " не найдена"));
        Optional<Event> eventOpt = eventRepository.findFirstByCategoryId(catId);
        if (eventOpt.isPresent()) {
            throw new ValidateException("Категория с id = " + catId + " используется");
        }
        repository.delete(category);
        log.info("Категория {} удалена", catId);
    }

    @Override
    public List<CategoryDto> findCategories(Pageable pageable) {
        return repository.findAll(pageable).stream()
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
