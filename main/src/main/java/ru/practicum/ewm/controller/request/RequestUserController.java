package ru.practicum.ewm.controller.request;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.service.request.RequestService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/requests")
@RequiredArgsConstructor
@Slf4j
@Validated
public class RequestUserController {
    private final RequestService requestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto create(@Positive @PathVariable Long userId,
                                          @Positive @RequestParam Long eventId) {
        log.info("Создать новую заявку пользователя {}, на мероприятие {}", userId, eventId);
        return requestService.create(userId, eventId);
    }

    @GetMapping
    public List<ParticipationRequestDto> getRequestsByUser(@Positive @PathVariable Long userId) {
        log.info("Получить все заявки пользователя {}", userId);
        return requestService.getRequestsByUser(userId);
    }

    @PatchMapping("/{requestId}/cancel")
    public ParticipationRequestDto cancel(@Positive @PathVariable Long userId,
                                          @Positive @PathVariable Long requestId) {
        log.info("Отмена заявки {} пользователем {}", requestId, userId);
        return requestService.cancelRequest(userId, requestId);
    }
}
