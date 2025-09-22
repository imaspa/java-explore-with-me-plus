package ru.practicum.ewm.service;

import com.querydsl.core.BooleanBuilder;
import org.springframework.util.StringUtils;
import ru.practicum.ewm.constant.EventState;
import ru.practicum.ewm.filter.EventsFilter;
import ru.practicum.ewm.model.QEvent;

import java.time.LocalDateTime;

public class EventPredicateBuilder {

    public static BooleanBuilder buildPredicate(EventsFilter filter, boolean forAdmin) {
        QEvent event = QEvent.event;
        BooleanBuilder predicate = new BooleanBuilder();

        // Только опубликованные — если не админ
        if (!forAdmin) {
            predicate.and(event.state.eq(EventState.PUBLISHED));
        }

        // Диапазон дат
        if (filter.getRangeStart() != null) {
            predicate.and(event.eventDate.goe(filter.getRangeStart()));
        } else if (!forAdmin) {
            predicate.and(event.eventDate.gt(LocalDateTime.now()));
        }

        if (filter.getRangeEnd() != null) {
            predicate.and(event.eventDate.loe(filter.getRangeEnd()));
        }

        // Категории
        if (filter.getCategories() != null && !filter.getCategories().isEmpty()) {
            predicate.and(event.category.id.in(filter.getCategories()));
        }

        // Платность (только для публичных)
        if (!forAdmin && filter.getPaid() != null) {
            predicate.and(event.paid.eq(filter.getPaid()));
        }

        // Текстовый поиск (только для публичных)
        if (!forAdmin && StringUtils.hasText(filter.getText())) {
            String searchText = "%" + filter.getText().toLowerCase() + "%";
            predicate.and(
                    event.annotation.likeIgnoreCase(searchText)
                            .or(event.description.likeIgnoreCase(searchText))
            );
        }

        // Only available (только для публичных)
        if (!forAdmin && filter.getOnlyAvailable() != null && filter.getOnlyAvailable()) {
            predicate.and(event.participantLimit.eq(0L)
                    .or(event.participantLimit.gt(10L))); // TODO: заменить на confirmedRequests
        }

        // Фильтр по пользователям (только для админов)
        if (forAdmin && filter.getUsers() != null && !filter.getUsers().isEmpty()) {
            predicate.and(event.initiator.id.in(filter.getUsers()));
        }

        // Фильтр по состояниям (только для админов)
        if (forAdmin && filter.getStates() != null && !filter.getStates().isEmpty()) {
            var states = filter.getStatesAsEnum();
            predicate.and(event.state.in(states));
        }

        return predicate;
    }
}