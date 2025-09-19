package ru.practicum.ewm.service.request;

import ru.practicum.ewm.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.dto.request.EventRequestStatusUpdateRequest;

import java.util.List;

public interface RequestService {
    ParticipationRequestDto create(Long userId, Long eventId);

    List<ParticipationRequestDto> getRequestsByUser(Long userId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    List<ParticipationRequestDto> getRequestsForEventOwner(Long ownerId, Long eventId);

    EventRequestStatusUpdateResult updateRequestStatus(Long ownerId,
                                                       Long eventId,
                                                       EventRequestStatusUpdateRequest updateDto);
}
