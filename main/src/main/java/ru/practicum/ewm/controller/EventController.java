package ru.practicum.ewm.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.core.exception.ConditionsException;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.filter.EventsFilter;
import ru.practicum.ewm.service.EventService;

import java.util.List;

@RestController
@RequestMapping(path = "/events")
@RequiredArgsConstructor
@Slf4j
@Validated
public class EventController {
    private final EventService service;

    @GetMapping
    public List<EventShortDto> find(@RequestParam EventsFilter filter, @PageableDefault(page = 0, size = 10) Pageable pageable)
            throws ConditionsException {
        log.info("Поиск event'ов с фильтром {}; pagable: {}", filter, pageable);
        return null; //service.findPublicEventsWithFilter(filter, pageable);
    }

    @GetMapping("/{id}")
    public EventFullDto findById(@PathVariable @Positive Long id, HttpServletRequest request) {
        log.info("Получение подробной информации об опубликованном событии по eventId {}", id);
        EventFullDto result = service.findPublicEventById(id, request);
        log.info("Ответ: {}", result);
        return result;
    }

}
