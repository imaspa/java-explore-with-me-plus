package ru.practicum.ewm.service.event;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.NumberPath;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import ru.practicum.ewm.EndpointHitDto;
import ru.practicum.ewm.ViewStatsDto;
import ru.practicum.ewm.client.StatsClient;
import ru.practicum.ewm.core.exception.NotFoundException;
import ru.practicum.ewm.core.exception.ValidateException;
import ru.practicum.ewm.core.exception.WrongRequestException;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventParamAdminDto;
import ru.practicum.ewm.dto.event.EventParamDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.dto.event.LocationDto;
import ru.practicum.ewm.dto.event.NewEventDto;
import ru.practicum.ewm.dto.event.UpdateEventDto;
import ru.practicum.ewm.mapper.EventMapper;
import ru.practicum.ewm.mapper.LocationMapper;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.EventSort;
import ru.practicum.ewm.model.EventState;
import ru.practicum.ewm.model.EventStateAction;
import ru.practicum.ewm.model.Location;
import ru.practicum.ewm.model.QEvent;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.LocationRepository;
import ru.practicum.ewm.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@Validated
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final LocationMapper locationMapper;
    private final StatsClient statsClient;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public EventFullDto create(NewEventDto newEventDto, Long userId) {

        User user = getUserOrThrow(userId);

        if (newEventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidateException("Время начала события должно быть минимум на 2 часа больше текущего");
        }

        Long catId = newEventDto.getCategory();
        Category category = getCategoryOrThrow(catId);

        LocationDto locationDto = newEventDto.getLocation();
        Location location = locationRepository.findFirstByLatAndLon(locationDto.getLat(), locationDto.getLon())
                .orElse(locationRepository.save(locationMapper.toEntity(locationDto)));

        // requestModeration: Если true, то все заявки будут ожидать подтверждения инициатором события.
        // Если false - то будут подтверждаться автоматически ???
        EventState state;
        LocalDateTime publishedOn;
        if (newEventDto.getRequestModeration()) {
            state = EventState.PENDING;
            publishedOn = null;
        } else {
            state = EventState.PENDING; //PUBLISHED;
            publishedOn = null;
            //publishedOn = LocalDateTime.now();
        }

        Event event = EventMapper.newDtoToEvent(newEventDto, user, category, location, state, publishedOn);
        event = eventRepository.save(event);
        log.info("Создан event, id = {}", event.getId());
        return EventMapper.eventToFullDto(event, 0L, 0L);
    }

    @Override
    @Transactional
    public EventFullDto update(UpdateEventDto updateEventDto) {
        Long userId = updateEventDto.getUserId();
        getUserOrThrow(userId);

        Long eventId = updateEventDto.getId();
        Event event = getEventOrThrow(eventId, userId);

        // изменить можно только отмененные события или события в состоянии ожидания модерации (Ожидается код ошибки 409)
        if (event.getState().equals(EventState.PUBLISHED)) {
            throw new ValidateException("Нельзя изменять событие в статусе PUBLISHED");
        }

        Long catId = updateEventDto.getCategory();
        if (catId != null) {
            Category category = getCategoryOrThrow(catId);
            event.setCategory(category);
        }
        if (updateEventDto.getAnnotation() != null) {
            event.setAnnotation(updateEventDto.getAnnotation());
        }
        if (updateEventDto.getDescription() != null) {
            event.setDescription(updateEventDto.getDescription());
        }
        if (updateEventDto.getEventDate() != null) {
            if (updateEventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
                throw new WrongRequestException("Время начала события должно быть минимум на 2 часа больше текущего");
            }
            event.setEventDate(updateEventDto.getEventDate());
        }

        LocationDto locationDto = updateEventDto.getLocation();
        if (locationDto != null) {
            Location location = locationRepository.findFirstByLatAndLon(locationDto.getLat(), locationDto.getLon())
                    .orElse(locationRepository.save(locationMapper.toEntity(locationDto)));
            event.setLocation(location);
        }

        if (updateEventDto.getPaid() != null) {
            event.setPaid(updateEventDto.getPaid());
        }
        if (updateEventDto.getParticipantLimit() != null) {
            event.setParticipantLimit(updateEventDto.getParticipantLimit());
        }
        if (updateEventDto.getRequestModeration() != null) {
            event.setRequestModeration(updateEventDto.getRequestModeration());
        }

        if (updateEventDto.getStateAction() != null) {
            EventStateAction stateAct = updateEventDto.getStateAction();
            switch (stateAct) {
                case SEND_TO_REVIEW:
                    event.setState(EventState.PENDING);
                    break;
                case CANCEL_REVIEW:
                    event.setState(EventState.CANCELED);
                    break;
                default:
                    throw new WrongRequestException("Статус " + stateAct + " не предусмотрен");
            }
        }
        if (updateEventDto.getTitle() != null) {
            event.setTitle(updateEventDto.getTitle());
        }
        log.info("Изменен event, id = {}", eventId);
        event = eventRepository.save(event);
        // TODO получить confirmedRequests
        Long confirmedRequests = getConfirmedRequests(eventId);
        Long views = getViews(eventId);
        return EventMapper.eventToFullDto(event, confirmedRequests, views);

    }

    @Override
    @Transactional
    public EventFullDto updateAdmin(UpdateEventDto updateEventDto) {
        Long eventId = updateEventDto.getId();
        if (eventId == null) {
            throw new ValidateException("Передан eventId == null");
        }
        Event event = getEventOrThrow(eventId);

        EventState state = event.getState(); // PENDING, PUBLISHED, CANCELED
        EventStateAction stateAct = updateEventDto.getStateAction(); // PUBLISH_EVENT, REJECT_EVENT

        if (stateAct != null) {
            //событие можно публиковать, только если оно в состоянии ожидания публикации (Ожидается код ошибки 409)
            if (stateAct.equals(EventStateAction.PUBLISH_EVENT)) {
                if (state.equals(EventState.PENDING)) {
                    event.setState(EventState.PUBLISHED);
                } else if (state.equals(EventState.CANCELED)) {
                    throw new ValidateException("Нельзя публиковать событие, находящееся в статусе " + state);
                } else if (state.equals(EventState.PUBLISHED)) {
                    throw new ValidateException("Нельзя публиковать событие, находящееся в статусе " + state);
                }
            }
            // событие можно отклонить, только если оно еще не опубликовано (Ожидается код ошибки 409)
            if (stateAct.equals(EventStateAction.REJECT_EVENT)) {
                if (state.equals(EventState.PUBLISHED)) {
                    throw new ValidateException("Нельзя отклонить опубликованное событие " + state);
                } else {
                    event.setState(EventState.CANCELED);
                }
            }
        }
        // дата начала изменяемого события должна быть не ранее чем за час от даты публикации. (Ожидается код ошибки 409)
        LocalDateTime newEventDate = updateEventDto.getEventDate();
        log.info("newEventDate = {}", newEventDate);
        log.info("старая eventDate = {}", event.getEventDate());
        if (newEventDate != null) {
            if (stateAct != null) {
                if (stateAct.equals(EventStateAction.PUBLISH_EVENT)) {
                    if (newEventDate.isBefore(LocalDateTime.now().plusHours(1))) {
                        throw new ValidateException("Время начала изменяемого события должно быть минимум на 1 час больше текущего (публикуем)");
                    }
                }
            } else {
                if (state.equals(EventState.PUBLISHED) && event.getPublishedOn() != null) {
                    if (newEventDate.isBefore(event.getPublishedOn().plusHours(1))) {
                        throw new ValidateException("Время начала изменяемого события должно быть минимум на 1 час больше существующей даты публикации");
                    }
                }
            }
            event.setEventDate(newEventDate);
            log.info("новая eventDate = {}", event.getEventDate());
        }
        /*else {
            if (stateAct != null) {
                if (stateAct.equals(EventStateAction.PUBLISH_EVENT) && event.getPublishedOn() != null) {
                    if (event.getPublishedOn().isBefore(LocalDateTime.now().plusHours(1))) {
                        throw new ValidateException("Существующее время начала события должно быть минимум на 1 час больше текущего (публикуем)");
                    }
                }
            }
        }*/

        Long catId = updateEventDto.getCategory();
        if (catId != null) {
            Category category = getCategoryOrThrow(catId);
            event.setCategory(category);
        }
        if (updateEventDto.getAnnotation() != null) {
            event.setAnnotation(updateEventDto.getAnnotation());
        }
        if (updateEventDto.getDescription() != null) {
            event.setDescription(updateEventDto.getDescription());
        }
        LocationDto locationDto = updateEventDto.getLocation();
        if (locationDto != null) {
            Location location = locationRepository.findFirstByLatAndLon(locationDto.getLat(), locationDto.getLon())
                    .orElse(locationRepository.save(locationMapper.toEntity(locationDto)));
            event.setLocation(location);
        }
        if (updateEventDto.getPaid() != null) {
            event.setPaid(updateEventDto.getPaid());
        }
        if (updateEventDto.getParticipantLimit() != null) {
            event.setParticipantLimit(updateEventDto.getParticipantLimit());
        }
        if (updateEventDto.getRequestModeration() != null) {
            event.setRequestModeration(updateEventDto.getRequestModeration());
        }
        if (updateEventDto.getTitle() != null) {
            event.setTitle(updateEventDto.getTitle());
        }

        log.info("Админ изменилн event, id = {}", eventId);
        event = eventRepository.save(event);
        // TODO получить confirmedRequests
        Long confirmedRequests = getConfirmedRequests(eventId);
        Long views = getViews(eventId);
        return EventMapper.eventToFullDto(event, confirmedRequests, views);
    }

    @Override
    public EventFullDto findById(Long userId, Long eventId) {
        getUserOrThrow(userId);
        Event event = getEventOrThrow(eventId, userId);
        // TODO получить confirmedRequests
        Long confirmedRequests = getConfirmedRequests(eventId);
        Long views = getViews(eventId);
        log.info("Нашли event {} юзера {}", eventId, userId);
        return EventMapper.eventToFullDto(event, confirmedRequests, views);
    }

    @Override
    public List<EventShortDto> findByUserId(Long userId, Pageable pageable) {
        getUserOrThrow(userId);
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdOn")
        );
        return eventRepository.findAllByInitiatorId(userId, sortedPageable)
                .stream()
                .map(event -> EventMapper.EventToShortDto(event,
                        getConfirmedRequests(event.getId()),
                        getViews(event.getId())
                ))
                .toList();
    }

    @Override
    public List<EventShortDto> findPublicEventsWithFilter(EventParamDto eventParamDto, HttpServletRequest request) {
        LocalDateTime rangeStart = eventParamDto.getRangeStart();
        LocalDateTime rangeEnd = eventParamDto.getRangeEnd();
        if (rangeStart != null && rangeEnd != null && rangeEnd.isBefore(rangeStart)) {
            throw new WrongRequestException("Дата окончания события должна быть больше даты начала");
        }

        QEvent event = QEvent.event;
        BooleanBuilder predicate = new BooleanBuilder();

        predicate.and(event.state.eq(EventState.PUBLISHED));

        // String text
        log.info("получен text = {}", eventParamDto.getText());
        if (StringUtils.hasText(eventParamDto.getText())) {
            String searchText = "%" + eventParamDto.getText().toLowerCase() + "%";
            predicate.and(
                    event.annotation.likeIgnoreCase(searchText)
                            .or(event.description.likeIgnoreCase(searchText))
            );
        }
        // List<Long> categories
        log.info("получены categories = {}", eventParamDto.getCategories());
        if (eventParamDto.getCategories() != null && !eventParamDto.getCategories().isEmpty()) {
            predicate.and(event.category.id.in(eventParamDto.getCategories()));
        }
        // Boolean paid
        if (eventParamDto.getPaid() != null) {
            predicate.and(event.paid.eq(eventParamDto.getPaid()));
        }
        // Boolean onlyAvailable
        if (eventParamDto.getOnlyAvailable() != null && eventParamDto.getOnlyAvailable().equals(true)) {
            predicate.and(event.participantLimit.eq(0L)
                    .or(event.participantLimit.gt(10L))); // TODO должен быть больше, чем confirmedRequests ??
        }
        // LocalDateTime rangeStart;
        if (rangeStart != null) {
            predicate.and(event.eventDate.after(rangeStart).or(event.eventDate.eq(rangeStart)));
        } else {
            predicate.and(event.eventDate.after(LocalDateTime.now()));
        }
        // LocalDateTime rangeEnd
        if (rangeEnd != null) {
            predicate.and(event.eventDate.before(rangeEnd).or(event.eventDate.eq(rangeEnd)));
        }

        Pageable pageable = eventParamDto.getPageable();
        // получить список событий
        Page<Event> eventsPage = eventRepository.findAll(predicate, pageable);
        if (eventsPage.isEmpty()) {
            log.info("Получен пустой список event'ов");
            return new ArrayList<>();
        }

        // подготовить uris для запроса статистики
        List<String> uris = eventsPage.stream()
                .map(eventPage -> "/events/" + eventPage.getId())
                .toList();

        // получить список статистики по всем событиям одним обращением и добавить в EventShortDto
        // поместить статистику в map с ключем uri
        Map<String, Long> viewsUriMap = getViews(uris);

        EventSort sort = eventParamDto.getSort();
        Comparator<EventShortDto> comparator;
        if (sort == null) {
            comparator = Comparator.comparing(EventShortDto::getEventDate).reversed();
        } else {
            if (sort.equals(EventSort.EVENT_DATE)) {
                comparator = Comparator.comparing(EventShortDto::getEventDate).reversed();
            } else {
                comparator = Comparator.comparing(EventShortDto::getViews).reversed();
            }
        }

        List<EventShortDto> result = eventsPage
                .stream()
                .map(eventFind -> createEventShortDto(eventFind, viewsUriMap))
                .sorted(comparator)
                .toList();
        log.info("Получен список event'ов: {}", result);
        return result;
    }

    @Override
    public List<EventFullDto> findAdminEventsWithFilter(EventParamAdminDto eventParamDto, HttpServletRequest request) {
        LocalDateTime rangeStart = eventParamDto.getRangeStart();
        LocalDateTime rangeEnd = eventParamDto.getRangeEnd();
        if (rangeStart != null && rangeEnd != null && rangeEnd.isBefore(rangeStart)) {
            throw new ValidateException("Дата окончания события должна быть больше даты начала");
        }

        QEvent event = QEvent.event;
        BooleanBuilder predicate = new BooleanBuilder();

        // List<Long> users;
        if (eventParamDto.getUsers() != null && !eventParamDto.getUsers().isEmpty()) {
            predicate.and(event.initiator.id.in(eventParamDto.getUsers()));
        }

        // List<String> states
        if (eventParamDto.getStates() != null && !eventParamDto.getStates().isEmpty()) {
            List<EventState> states = eventParamDto.getStates().stream()
                    .map(EventState::valueOf)
                    .toList();
            predicate.and(event.state.in(states));
        }

        // List<Long> categories
        if (eventParamDto.getCategories() != null && !eventParamDto.getCategories().isEmpty()) {
            predicate.and(event.category.id.in(eventParamDto.getCategories()));
        }
        // LocalDateTime rangeStart;
        if (rangeStart != null) {
            predicate.and(event.eventDate.after(rangeStart).or(event.eventDate.eq(rangeStart)));
        }
        // LocalDateTime rangeEnd
        if (rangeEnd != null) {
            predicate.and(event.eventDate.before(rangeEnd).or(event.eventDate.eq(rangeEnd)));
        }

        Pageable pageable = eventParamDto.getPageable();
        // получить список событий
        Page<Event> eventsPage = eventRepository.findAll(predicate, pageable);
        if (eventsPage.isEmpty()) {
            log.info("Получен пустой список event'ов");
            return new ArrayList<>();
        }

        // подготовить uris для запроса статистики
        List<String> uris = eventsPage.stream()
                .map(eventPage -> "/events/" + eventPage.getId())
                .toList();

        // получить список статистики по всем событиям одним обращением и добавить в EventShortDto
        // поместить статистику в map с ключем uri
        Map<String, Long> viewsUriMap = getViews(uris);

        List<EventFullDto> result = eventsPage
                .stream()
                .map(eventFind -> createEventFullDto(eventFind, viewsUriMap))
                .toList();
        log.info("Получен список event'ов: {}", result);
        return result;
    }

    @Override
    public EventFullDto findPublicEventById(Long eventId, HttpServletRequest request) {
        Event event = getEventOrThrow(eventId, EventState.PUBLISHED);
        log.info("Нашли опубликованный event {}", eventId);
        // TODO получить confirmedRequests
        Long confirmedRequests = getConfirmedRequests(eventId);
        Long views = getViews(eventId);
        saveHit(request);
        return EventMapper.eventToFullDto(event, confirmedRequests, views);
    }

    private EventShortDto createEventShortDto(Event event, Map<String, Long> viewsUriMap) {
        Long eventId = event.getId();
        String eventUri = "/events/" + eventId;
        Long hits = viewsUriMap.getOrDefault(eventUri, 0L);
        return EventMapper.EventToShortDto(
                event,
                getConfirmedRequests(eventId),
                hits
        );
    }

    private EventFullDto createEventFullDto(Event event, Map<String, Long> viewsUriMap) {
        Long eventId = event.getId();
        String eventUri = "/events/" + eventId;
        Long hits = viewsUriMap.getOrDefault(eventUri, 0L);
        return EventMapper.eventToFullDto(
                event,
                getConfirmedRequests(eventId),
                hits
        );
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new WrongRequestException("Юзер с id = " + userId + " не найден"));
    }

    private Category getCategoryOrThrow(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new WrongRequestException("Категория с id = " + catId + " не найдена"));
    }

    private Event getEventOrThrow(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event с id = " + eventId + " не найден"));
        return event;
    }

    private Event getEventOrThrow(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event с id = " + eventId + " не найден"));
        if (!event.getInitiator().getId().equals(userId)) {
            throw new WrongRequestException("Юзер с id = " + userId + " не инициатор event'а " + eventId);
        }
        return event;
    }

    private Event getEventOrThrow(Long eventId, EventState state) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event с id = " + eventId + " не найден"));
        log.info("getEventOrThrow  state {}", state);
        log.info("event.getState() {}", event.getState());
        if (!event.getState().equals(state)) {
            throw new NotFoundException("Event с id = " + eventId + " не опубликован");
        }
        return event;
    }

    private Long getConfirmedRequests(Long eventId) {
        // TODO получить confirmedRequests
        return 0L;
    }

    private Long getViews(Long eventId) {
        String start = LocalDateTime.now().minusYears(20L).format(formatter);
        String end = LocalDateTime.now().format(formatter);
        ArrayList<String> urls = new ArrayList<>();
        urls.add("/events/" + eventId);
        List<ViewStatsDto> listDto = statsClient.getStats(start, end, urls.toArray(new String[0]), true);
        if (listDto.isEmpty()) {
            return 0L;
        }
        return listDto.getFirst().getHits();
    }

    private Map<String, Long> getViews(List<String> uris) {
        // получить список статистики по всем событиям одним обращением
        String start = LocalDateTime.now().minusYears(20L).format(formatter);
        String end = LocalDateTime.now().format(formatter);
        List<ViewStatsDto> listStatDto = statsClient.getStats(start, end, uris.toArray(new String[0]), true);
        // поместить статистику в map с ключем uri
        return listStatDto.stream()
                .collect(Collectors.toMap(
                        ViewStatsDto::getUri,
                        ViewStatsDto::getHits)
                );
    }

    private void saveHit(HttpServletRequest request) {
        try {
            String clientIp = request.getRemoteAddr();
            String requestUri = request.getRequestURI();

            log.info("Сохранение статистики : ip = {}, Endpoint: {}", clientIp, requestUri);

            EndpointHitDto hitDto = EndpointHitDto.builder()
                    .app("main-service")
                    .uri(requestUri)
                    .ip(clientIp)
                    .timestamp(LocalDateTime.now())
                    .build();
            statsClient.saveHit(hitDto);
        } catch (Exception e) {
            log.error("Не удалось сохранить статистику: {}", e.getMessage());
        }
    }
}
