package ru.practicum.ewm.service;

import com.querydsl.core.BooleanBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ru.practicum.ewm.constant.EventSort;
import ru.practicum.ewm.constant.EventState;
import ru.practicum.ewm.constant.EventStateAction;
import ru.practicum.ewm.core.exception.ConditionsException;
import ru.practicum.ewm.core.exception.NotFoundException;
import ru.practicum.ewm.core.exception.ValidateException;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.dto.event.EventLocationDto;
import ru.practicum.ewm.dto.event.EventNewDto;
import ru.practicum.ewm.dto.event.EventUpdateDto;
import ru.practicum.ewm.filter.EventsFilter;
import ru.practicum.ewm.mapper.EventMapper;
import ru.practicum.ewm.mapper.EventMapperOld;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.Location;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;

@Slf4j
@Service
@Transactional(readOnly = true)
@Validated
@RequiredArgsConstructor
public class EventService {

    private final EventRepository repository;
    private final EventMapper mapper;


    private final UserRepository userRepository;

    private final LocationService locationService;


    private final CategoryRepository categoryRepository;

    private final StatsService statsService;

    // ========================
    // CREATE / UPDATE
    // ========================

    @Transactional
    public EventFullDto create(EventNewDto dto, Long userId) throws ConditionsException {
        User user = getUserOrThrow(userId);
        Category category = getCategoryOrThrow(dto.getCategory());
        Location location = locationService.getOrCreateLocation(dto.getLocation());

        Event event = mapper.toEntityWithNewDto(dto, user, category, location);
        event = repository.save(event);
        log.info("Создано событие с id = {}", event.getId());

        return mapper.toDto(event);
    }

    @Transactional
    public EventFullDto update(Long userId, Long eventId, EventUpdateDto dto) throws ConditionsException {
        if (!userIsExist(userId)) {
            throw new NotFoundException("Пользователь не найден");
        }
        Event event = getEventOrThrow(eventId, userId);
        if (event.getState() == EventState.PUBLISHED) {
            throw new ValidateException("Нельзя изменять опубликованное событие");
        }

        Category category = (dto.getCategory() == null) ? null : getCategoryOrThrow(dto.getCategory());
        Location location = (dto.getLocation() == null) ? null : locationService.getOrCreateLocation(dto.getLocation());
        EventState state = dto.getStateAction() == null ?
                null :
                switch (dto.getStateAction()) {
                    case PUBLISH_EVENT, REJECT_EVENT -> null;
                    case SEND_TO_REVIEW -> EventState.PENDING;
                    case CANCEL_REVIEW -> EventState.CANCELED;
                };
        event = mapper.toEntityWithUpdateDto(event, dto, category, location, state);
        event = repository.save(event);
        log.info("Обновлено событие с id = {}", eventId);

        Long calcConfirmedRequests = getConfirmedRequests(eventId);
        Long calcView = statsService.getViewsForEvent(eventId);
        return mapper.toDto(event).toBuilder()
                .confirmedRequests(calcConfirmedRequests)
                .views(calcView)
                .build();
    }

    @Transactional
    public EventFullDto updateAdmin(EventUpdateDto updateEventDto) throws ConditionsException {
        Long eventId = updateEventDto.getId();
        if (eventId == null) {
            throw new ValidateException("eventId не может быть null");
        }

        Event event = getEventOrThrow(eventId);
        EventState currentState = event.getState();
        EventStateAction action = updateEventDto.getStateAction();

        if (action != null) {
            if (action == EventStateAction.PUBLISH_EVENT) {
                if (currentState != EventState.PENDING) {
                    throw new ValidateException("Можно публиковать только события в состоянии PENDING");
                }
                event.setState(EventState.PUBLISHED);
            } else if (action == EventStateAction.REJECT_EVENT) {
                if (currentState == EventState.PUBLISHED) {
                    throw new ValidateException("Нельзя отклонить опубликованное событие");
                }
                event.setState(EventState.CANCELED);
            }
        }

        if (updateEventDto.getEventDate() != null) {
            LocalDateTime newDate = updateEventDto.getEventDate();
            if (action == EventStateAction.PUBLISH_EVENT) {
                if (newDate.isBefore(LocalDateTime.now().plusHours(1))) {
                    throw new ValidateException("Дата начала должна быть не ранее чем через 1 час при публикации");
                }
            } else if (currentState == EventState.PUBLISHED && event.getPublishedOn() != null) {
                if (newDate.isBefore(event.getPublishedOn().plusHours(1))) {
                    throw new ValidateException("Дата начала должна быть не ранее чем через 1 час после публикации");
                }
            }
            event.setEventDate(newDate);
        }

        if (updateEventDto.getCategory() != null) {
            event.setCategory(getCategoryOrThrow(updateEventDto.getCategory()));
        }
        if (updateEventDto.getAnnotation() != null) event.setAnnotation(updateEventDto.getAnnotation());
        if (updateEventDto.getDescription() != null) event.setDescription(updateEventDto.getDescription());
        if (updateEventDto.getLocation() != null) {
            EventLocationDto locDto = updateEventDto.getLocation();
            Location location = locationRepository.findFirstByLatAndLon(locDto.getLat(), locDto.getLon())
                    .orElse(locationRepository.save(locationMapper.toEntity(locDto)));
            event.setLocation(location);
        }
        if (updateEventDto.getPaid() != null) event.setPaid(updateEventDto.getPaid());
        if (updateEventDto.getParticipantLimit() != null) event.setParticipantLimit(updateEventDto.getParticipantLimit());
        if (updateEventDto.getRequestModeration() != null) event.setRequestModeration(updateEventDto.getRequestModeration());
        if (updateEventDto.getTitle() != null) event.setTitle(updateEventDto.getTitle());

        event = repository.save(event);
        log.info("Администратор обновил событие с id = {}", eventId);

        Long confirmedRequests = getConfirmedRequests(eventId);
        Long views = statsService.getViewsForEvent(eventId);
        return EventMapperOld.eventToFullDto(event, confirmedRequests, views);
    }

    // ========================
    // GET BY ID
    // ========================

    public EventFullDto findById(Long userId, Long eventId) throws ConditionsException {
        if (!userIsExist(userId)) {
            throw new NotFoundException("Пользователь не найден");
        }
        Event event = getEventOrThrow(eventId, userId);
        Long confirmedRequests = getConfirmedRequests(eventId);
        Long views = statsService.getViewsForEvent(eventId);
        log.info("Получено событие {} пользователя {}", eventId, userId);
        return EventMapperOld.eventToFullDto(event, confirmedRequests, views);
    }

    public EventFullDto findPublicEventById(Long eventId, HttpServletRequest request) {
        Event event = getEventOrThrow(eventId, EventState.PUBLISHED);
        Long confirmedRequests = getConfirmedRequests(eventId);
        Long views = statsService.getViewsForEvent(eventId);

        statsService.saveHit(
                "main-service",
                request.getRequestURI(),
                request.getRemoteAddr(),
                LocalDateTime.now()
        );

        log.info("Получено публичное событие {}", eventId);
        return EventMapperOld.eventToFullDto(event, confirmedRequests, views);
    }

    // ========================
    // GET BY USER
    // ========================

    public List<EventShortDto> findByUserId(Long userId, Pageable pageable) throws ConditionsException {
        getUserOrThrow(userId);
        Pageable sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdOn")
        );

        return repository.findAllByInitiatorId(userId, sorted)
                .stream()
                .map(event -> EventMapperOld.EventToShortDto(
                        event,
                        getConfirmedRequests(event.getId()),
                        statsService.getViewsForEvent(event.getId())
                ))
                .toList();
    }

    // ========================
    // SEARCH (Public & Admin)
    // ========================

    public List<EventShortDto> findPublicEventsWithFilter(@Valid EventsFilter filter, Pageable pageable) {
        return findEventsWithFilterInternal(
                filter,
                pageable,
                false,
                (event, viewsMap) -> {
                    String uri = "/events/" + event.getId();
                    Long views = viewsMap.getOrDefault(uri, 0L);
                    return EventMapperOld.EventToShortDto(event, getConfirmedRequests(event.getId()), views);
                }
        );
    }

    public List<EventFullDto> findAdminEventsWithFilter(@Valid EventsFilter filter, Pageable pageable) {
        return findEventsWithFilterInternal(
                filter,
                pageable,
                true,
                (event, viewsMap) -> {
                    String uri = "/events/" + event.getId();
                    Long views = viewsMap.getOrDefault(uri, 0L);
                    return EventMapperOld.eventToFullDto(event, getConfirmedRequests(event.getId()), views);
                }
        );
    }

    private <T> List<T> findEventsWithFilterInternal(
            EventsFilter filter,
            Pageable pageable,
            boolean forAdmin,
            BiFunction<Event, Map<String, Long>, T> mapper) {

        BooleanBuilder predicate = EventPredicateBuilder.buildPredicate(filter, forAdmin);
        Page<Event> eventsPage = repository.findAll(predicate, pageable);

        if (eventsPage.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> uris = eventsPage.stream()
                .map(e -> "/events/" + e.getId())
                .toList();

        Map<String, Long> viewsUriMap = statsService.getViewsForUris(uris);
        Stream<Event> eventStream = eventsPage.stream();

        // Сортировка — только для публичного режима
        if (!forAdmin) {
            EventSort sort = getEventSort(pageable);
            if (EventSort.VIEWS == sort) {
                eventStream = eventStream.sorted((e1, e2) -> {
                    Long v1 = viewsUriMap.getOrDefault("/events/" + e1.getId(), 0L);
                    Long v2 = viewsUriMap.getOrDefault("/events/" + e2.getId(), 0L);
                    return v2.compareTo(v1); // по убыванию просмотров
                });
            } else {
                eventStream = eventStream.sorted(Comparator.comparing(Event::getEventDate).reversed());
            }
        }

        List<T> result = eventStream
                .map(e -> mapper.apply(e, viewsUriMap))
                .toList();

        log.info("Найдено {} событий в режиме {}", result.size(), forAdmin ? "ADMIN" : "PUBLIC");
        return result;
    }

    private EventSort getEventSort(Pageable pageable) {
        try {
            return EventSort.valueOf(pageable.getSort().toString());
        } catch (Exception e) {
            return EventSort.EVENT_DATE;
        }
    }

    // ========================
    // HELPERS
    // ========================

    private User getUserOrThrow(Long userId) throws ConditionsException {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ConditionsException("Пользователь с id=" + userId + " не найден"));
    }

    private Category getCategoryOrThrow(Long catId) throws ConditionsException {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new ConditionsException("Категория с id=" + catId + " не найдена"));
    }

    private Event getEventOrThrow(Long eventId) {
        return repository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));
    }

    private Event getEventOrThrow(Long eventId, Long userId) throws ConditionsException {
        Event event = getEventOrThrow(eventId);
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConditionsException("Пользователь не является инициатором события");
        }
        return event;
    }

    private Event getEventOrThrow(Long eventId, EventState state) {
        Event event = getEventOrThrow(eventId);
        if (event.getState() != state) {
            throw new NotFoundException("Событие не в состоянии " + state);
        }
        return event;
    }

    private Long getConfirmedRequests(Long eventId) {
        // TODO: Реализовать через ParticipationRequestRepository
        return 0L;
    }

    @Transactional(readOnly = true)
    public boolean userIsExist(Long userId) {
        return userRepository.existsById(userId);
    }
}