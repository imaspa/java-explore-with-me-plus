package ru.practicum.ewm.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.core.exception.ConditionsException;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.dto.event.EventNewDto;
import ru.practicum.ewm.dto.event.EventUpdateDto;
import ru.practicum.ewm.service.EventService;

import java.util.List;

@RestController
@RequestMapping(path = "/users/{userId}/events")
@RequiredArgsConstructor
@Slf4j
@Validated
public class UserEventsController {
    private final EventService service;

    @GetMapping
    public List<EventShortDto> find(
            @Positive @PathVariable Long userId,
            @PageableDefault(page = 0, size = 10) Pageable pageable) throws ConditionsException {
        log.info("Найти events пользователя {}, pagable:{}", userId, pageable);
        return service.findByUserId(userId, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto create(
            @Positive @PathVariable Long userId,
            @Valid @RequestBody EventNewDto newEventDto) throws ConditionsException {
        log.info("Пользователь {} создаёт event {}", userId, newEventDto);
        return service.create(newEventDto, userId);
    }

    @GetMapping("/{eventId}")
    public EventFullDto findById(
            @Positive @PathVariable Long userId,
            @Positive @PathVariable Long eventId) throws ConditionsException {
        log.info("Пользователь {} ищет event {}", userId, eventId);
        return service.findById(userId, eventId);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto update(
            @Positive @PathVariable Long userId,
            @Positive @PathVariable Long eventId,
            @RequestBody EventUpdateDto dto) throws ConditionsException {
        log.info("Пользователь {} изменяет event {} {}", userId, eventId, dto);
        return service.update(userId, eventId, dto);
    }










}
