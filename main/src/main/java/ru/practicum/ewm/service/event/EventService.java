package ru.practicum.ewm.service.event;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventParamAdminDto;
import ru.practicum.ewm.dto.event.EventParamDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.dto.event.NewEventDto;
import ru.practicum.ewm.dto.event.UpdateEventDto;

import java.util.List;

public interface EventService {
    EventFullDto create(NewEventDto newEventDto, Long userId);

    EventFullDto update(UpdateEventDto updateEventDto);

    EventFullDto findById(Long userId, Long eventId);

    List<EventShortDto> findByUserId(Long userId, Pageable pageable);

    List<EventShortDto> findPublicEventsWithFilter(EventParamDto eventParamDto, HttpServletRequest request);

    EventFullDto findPublicEventById(Long eventId, HttpServletRequest request);

    List<EventFullDto> findAdminEventsWithFilter(EventParamAdminDto eventParamDto, HttpServletRequest request);

    EventFullDto updateAdmin(UpdateEventDto updateEventDto);

}
