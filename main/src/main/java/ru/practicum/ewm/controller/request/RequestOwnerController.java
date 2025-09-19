package ru.practicum.ewm.controller.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.service.request.RequestService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/events/{eventId}/requests")
@RequiredArgsConstructor
@Slf4j
@Validated
public class RequestOwnerController {

    private final RequestService requestService;

    @GetMapping
    public List<ParticipationRequestDto> getForEvent(@Positive @PathVariable Long userId,
                                                     @Positive @PathVariable Long eventId) {
        log.info("Получение заявок на мероприятие {}", eventId);
        return requestService.getRequestsForEventOwner(userId, eventId);
    }

    @PatchMapping
    public EventRequestStatusUpdateResult updateStatus(@Positive @PathVariable Long userId,
                                                       @Positive @PathVariable Long eventId,
                                                       @Valid @RequestBody EventRequestStatusUpdateRequest updateDto) {
        log.info("Изменение статуса заявок на мероприятие {}, пользователем {}", eventId, userId);
        return requestService.updateRequestStatus(userId, eventId, updateDto);
    }
}
