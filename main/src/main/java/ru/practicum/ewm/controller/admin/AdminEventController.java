package ru.practicum.ewm.controller.admin;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.core.exception.ConditionsException;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventUpdateDto;
import ru.practicum.ewm.filter.EventsFilter;
import ru.practicum.ewm.service.EventService;

import java.util.List;

@RestController
@RequestMapping(path = "/admin/events")
@RequiredArgsConstructor
@Slf4j
public class AdminEventController {
    private final EventService service;

    @GetMapping
    public List<EventFullDto> find(@RequestParam EventsFilter filter, @PageableDefault(page = 0, size = 10) Pageable pageable) {
        log.info("Администратор. Поиск event'ов с фильтром {}; {}", filter, pageable);
        return null; //service.findAdminEventsWithFilter(filter, pageable);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto update(@Positive @PathVariable Long eventId, @RequestBody EventUpdateDto dto)
            throws ConditionsException {
        log.info("Администратор изменяет event {} {}", eventId, dto);
        EventUpdateDto updateEventDtoWithId = dto.toBuilder()
                .id(eventId)
                .build();
        EventFullDto result = service.updateAdmin(updateEventDtoWithId);
        log.info("Ответ: {}", result);
        return result;
    }
}
