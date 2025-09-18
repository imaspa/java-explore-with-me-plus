package ru.practicum.ewm.service.compilation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.core.exception.NotFoundException;
import ru.practicum.ewm.core.exception.ValidateException;
import ru.practicum.ewm.dto.compilation.CompilationDto;
import ru.practicum.ewm.dto.compilation.NewCompilationDto;
import ru.practicum.ewm.dto.compilation.UpdateCompilationRequest;
import ru.practicum.ewm.mapper.CompilationMapper;
import ru.practicum.ewm.model.Compilation;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.repository.CompilationRepository;
import ru.practicum.ewm.repository.EventRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;

    @Override
    @Transactional
    public CompilationDto create(NewCompilationDto dto) {
        Set<Event> events = new HashSet<>(getUniqueEvents(dto.getEvents()));
        Compilation compilation = compilationRepository.save(compilationMapper.toEntity(dto, events));
        log.info("Создана подборка, id = {}", compilation.getId());
        return compilationMapper.toDto(compilation);
    }

    @Override
    @Transactional
    public void delete(Long compId) {
        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Подборка с id " + compId + " не найдена");
        }
        compilationRepository.deleteById(compId);
        log.info("Удалена подборка id = {}", compId);
    }

    @Override
    @Transactional
    public CompilationDto update(Long compId, UpdateCompilationRequest dto) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id " + compId + " не найдена"));

        if (dto.getTitle() != null) {
            compilation.setTitle(dto.getTitle());
        }
        if (dto.getPinned() != null) {
            compilation.setPinned(dto.getPinned());
        }
        if (dto.getEvents() != null) {
            List<Event> events = getUniqueEvents(dto.getEvents());
            compilation.setEvents(new HashSet<>(events));
        }

        compilation = compilationRepository.save(compilation);
        log.info("Обновлена подборка id = {}", compId);
        return compilationMapper.toDto(compilation);
    }

    @Override
    public CompilationDto getById(Long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id " + compId + " не найдена"));
        return compilationMapper.toDto(compilation);
    }

    @Override
    public List<CompilationDto> getAll(Boolean pinned, Pageable pageable) {
        Page<Compilation> page;
        if (pinned != null) {
            log.info("Поиск подборок pinned={}", pinned);
            page = compilationRepository.findAllByPinned(pinned, pageable);
        } else {
            log.info("Поиск подборок без pinned");
            page = compilationRepository.findAll(pageable);
        }

        return page.stream()
                .map(compilationMapper::toDto)
                .toList();
    }

    private List<Event> getUniqueEvents(List<Long> eventsByDto) {
        List<Event> events = new ArrayList<>();
        if (eventsByDto != null && !eventsByDto.isEmpty()) {
            Set<Long> unique = new HashSet<>(eventsByDto);
            log.info("Проверка уникальности events");
            if (unique.size() != eventsByDto.size()) {
                throw new ValidateException("Список событий содержит дубликаты");
            }
            log.info("Сбор events для заполнения");
            events = eventRepository.findAllById(eventsByDto);
            if (events.size() != eventsByDto.size()) {
                throw new NotFoundException("Некоторые события не найдены");
            }
        }
        return events;
    }
}
