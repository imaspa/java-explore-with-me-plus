package ru.practicum.ewm.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventParamDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.model.EventSort;
import ru.practicum.ewm.service.event.EventService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping(path = "/events")
@RequiredArgsConstructor
@Slf4j
@Validated
public class EventControllerPublic {
    private final EventService eventService;

    @GetMapping
    public List<EventShortDto> searchEvents(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size,
            HttpServletRequest request) {
        EventParamDto eventParamDto = EventParamDto.builder()
                .text(text)
                .categories(categories)
                .paid(paid)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .onlyAvailable(onlyAvailable)
                .sort(sort == null ? null : EventSort.valueOf(sort.toUpperCase()))
                .pageable(PageRequest.of(from, size))
                .build();

        log.info("Поиск event'ов с фильтром {}", eventParamDto);

        return eventService.findPublicEventsWithFilter(eventParamDto, request);
    }

    @GetMapping("/{id}")
    public EventFullDto getEventById(@PathVariable @Positive Long id, HttpServletRequest request) {
        log.info("Получение подробной информации об опубликованном событии по eventId {}", id);
        EventFullDto result =  eventService.findPublicEventById(id, request);
        log.info("Ответ: {}", result);
        return result;
    }

}
