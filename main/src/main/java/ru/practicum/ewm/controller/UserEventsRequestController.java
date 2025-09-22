package ru.practicum.ewm.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.service.RequestService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/events/{eventId}/requests")
@RequiredArgsConstructor
@Slf4j
@Validated
public class UserEventsRequestController {

    private final RequestService service;

    @GetMapping
    public List<ParticipationRequestDto> findForEvent(
            @Positive @PathVariable Long userId,
            @Positive @PathVariable Long eventId) {
        log.info("Получение заявок на мероприятие {}", eventId);
        return service.getRequestsForEventOwner(userId, eventId);
    }

    @PatchMapping
    public EventRequestStatusUpdateResult update(
            @Positive @PathVariable Long userId,
            @Positive @PathVariable Long eventId,
            @Valid @RequestBody EventRequestStatusUpdateRequest dto) {
        log.info("Изменение статуса заявок на мероприятие {}, пользователем {}", eventId, userId);
        return service.updateRequestStatus(userId, eventId, dto);
    }
}
