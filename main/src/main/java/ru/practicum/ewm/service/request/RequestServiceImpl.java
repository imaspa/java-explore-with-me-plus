package ru.practicum.ewm.service.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.core.exception.NotFoundException;
import ru.practicum.ewm.core.exception.ValidateException;
import ru.practicum.ewm.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.mapper.RequestMapper;
import ru.practicum.ewm.model.*;
import ru.practicum.ewm.repository.RequestRepository;
import ru.practicum.ewm.repository.UserRepository;
import ru.practicum.ewm.repository.EventRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RequestMapper mapper;

    @Override
    @Transactional
    public ParticipationRequestDto create(Long userId, Long eventId) {
        if (requestRepository.findByEventIdAndRequesterId(eventId, userId).isPresent()) {
            throw new ValidateException("Запрос уже существует");
        }

        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Мероприятие с id=" + eventId + " не найдено"));

        if (Objects.equals(event.getInitiator().getId(), userId)) {
            throw new ValidateException("Нельзя подать заявку на своё мероприятие");
        }

        if (event.getState() != EventState.PUBLISHED) {
            throw new ValidateException("Подать заявку можно только на опубликованные мероприятия");
        }

        Long limit = event.getParticipantLimit();
        if (limit != null && limit > 0) {
            long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
            if (confirmedCount >= limit) {
                throw new ValidateException("Достигнут лимит участников");
            }
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .event(event)
                .requester(requester)
                .status(RequestStatus.PENDING)
                .build();

        Boolean moderation = event.getRequestModeration();
        if (moderation != null && !moderation) {
            request.setStatus(RequestStatus.CONFIRMED);
        }
        request = requestRepository.save(request);
        log.info("Создан запрос, id = {}", request.getId());
        return mapper.toDto(request);
    }

    @Override
    public List<ParticipationRequestDto> getRequestsByUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
        return requestRepository.findByRequesterId(userId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Заявка не найдена"));
        if (!Objects.equals(request.getRequester().getId(), userId)) {
            throw new ValidateException("Только владелец может отменить заявку");
        }
        request.setStatus(RequestStatus.CANCELED);
        request = requestRepository.save(request);
        log.info("Отменен запрос id = {}", requestId);
        return mapper.toDto(requestRepository.save(request));
    }

    @Override
    public List<ParticipationRequestDto> getRequestsForEventOwner(Long ownerId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Мероприятие с id=" + eventId + " не найдено"));
        if (!Objects.equals(event.getInitiator().getId(), ownerId)) {
            throw new ValidateException("Только владелец мероприятия может просматривать запросы на это мероприятие");
        }
        return requestRepository.findByEventId(eventId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long ownerId,
                                                              Long eventId,
                                                              EventRequestStatusUpdateRequest updateDto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Мероприятие с id=" + eventId + " не найдено"));

        if (!Objects.equals(event.getInitiator().getId(), ownerId)) {
            throw new ValidateException("Только владелец мероприятия может изменять статус запроса");
        }

        if (updateDto.getRequestIds() == null || updateDto.getRequestIds().isEmpty()) {
            return new EventRequestStatusUpdateResult(List.of(), List.of());
        }
        if (updateDto.getStatus() == null) {
            throw new ValidateException("Не указан статус");
        }

        if (updateDto.getStatus() == RequestStatus.CONFIRMED &&
                (!event.getRequestModeration() || event.getParticipantLimit() == 0)) {
            throw new ValidateException("Подтверждение заявок не требуется");
        }

        Long freeLimit = event.getParticipantLimit();
        if (freeLimit != null && freeLimit > 0) {
            freeLimit = freeLimit - requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
            if (freeLimit <= 0) {
                throw new ValidateException("Лимит по заявкам на данное событие уже достигнут");
            }
        }

        List<ParticipationRequest> requests =
                requestRepository.findAllByIdInAndStatus(updateDto.getRequestIds(), RequestStatus.PENDING);
        Map<Long, ParticipationRequest> map = requests.stream()
                .collect(Collectors.toMap(ParticipationRequest::getId, r -> r));

        List<Long> notFound = updateDto.getRequestIds().stream()
                .filter(id -> !map.containsKey(id))
                .toList();

        if (!notFound.isEmpty()) {
            throw new ValidateException("Не найдены заявки: " + notFound);
        }

        List<ParticipationRequest> toUpdate = new ArrayList<>();
        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        for (Long id : updateDto.getRequestIds()) {
            ParticipationRequest r = map.get(id);

            if (updateDto.getStatus() == RequestStatus.CONFIRMED) {
                if (freeLimit != null && freeLimit <= 0) {
                    r.setStatus(RequestStatus.REJECTED);
                    rejected.add(mapper.toDto(r));
                    log.info("Заявка {} будет отклонена", r.getId());
                } else {
                    if (freeLimit != null) {
                        freeLimit--;
                    }
                    r.setStatus(RequestStatus.CONFIRMED);
                    confirmed.add(mapper.toDto(r));
                    log.info("Заявка {} будет подтверждена", r.getId());
                }
            } else if (updateDto.getStatus() == RequestStatus.REJECTED) {
                r.setStatus(RequestStatus.REJECTED);
                rejected.add(mapper.toDto(r));
            } else {
                throw new ValidateException("Доступны только статусы CONFIRMED или REJECTED");
            }
            toUpdate.add(r);
        }

        if (!toUpdate.isEmpty()) {
            requestRepository.saveAll(toUpdate);
            log.info("Список заявок обновлен");
        }

        if (freeLimit != null && freeLimit == 0 && event.getParticipantLimit() > 0) {
            List<ParticipationRequest> pendingRequests =
                    requestRepository.findByEventIdAndStatus(eventId, RequestStatus.PENDING);
            if (!pendingRequests.isEmpty()) {
                pendingRequests.forEach(r -> r.setStatus(RequestStatus.REJECTED));
                List<ParticipationRequest> rejectedRequests = requestRepository.saveAll(pendingRequests);
                rejected.addAll(rejectedRequests.stream().map(mapper::toDto).toList());
                log.info("Был достигнут лимит заявок, все оставшиеся PENDING, переведены в REJECTED");
            }
        }

        return new EventRequestStatusUpdateResult(confirmed, rejected);
    }
}
