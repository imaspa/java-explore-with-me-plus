package ru.practicum.ewm.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.dto.event.NewEventDto;
import ru.practicum.ewm.dto.event.UpdateEventDto;
import ru.practicum.ewm.service.event.EventService;

import java.util.List;

@RestController
@RequestMapping(path = "/users/{userId}/events")
@RequiredArgsConstructor
@Slf4j
@Validated
public class EventControllerPrivate {
    private final EventService eventService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto create(@Positive @PathVariable Long userId,
                               @Valid @RequestBody NewEventDto newEventDto) {
        log.info("Пользователь {} создаёт event {}", userId, newEventDto);
        return eventService.create(newEventDto, userId);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto update(@Positive @PathVariable Long userId,
                               @Positive @PathVariable Long eventId,
                               @Valid @RequestBody UpdateEventDto updateEventDto) {
        log.info("Пользователь {} изменяет event {} {}", userId, eventId, updateEventDto);
        UpdateEventDto updateEventDtoWithId = updateEventDto.toBuilder()
                .id(eventId)
                .userId(userId)
                .build();
        //updateEventDto.toBuilder().id(eventId).userId(userId).build();
        log.info("updateEventDtoWithId {}", updateEventDtoWithId);
        return eventService.update(updateEventDtoWithId);
    }

    @GetMapping("/{eventId}")
    public EventFullDto findById(@Positive @PathVariable Long userId,
                                 @Positive @PathVariable Long eventId) {
        log.info("Пользователь {} ищет event {}", userId, eventId);
        return eventService.findById(userId, eventId);
    }

    @GetMapping
    public List<EventShortDto> findByUserId(@Positive @PathVariable Long userId,
                                            @RequestParam(name = "from", defaultValue = "0") int from,
                                            @RequestParam(name = "size", defaultValue = "10") int size) {
        log.info("Найти events пользователя {}, from = {}, size = {}", userId, from, size);
        Pageable pageable = PageRequest.of(from, size);
        return eventService.findByUserId(userId, pageable);
    }

}
