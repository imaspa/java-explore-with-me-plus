package ru.practicum.ewm.repository;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.filter.EventsFilter;
import ru.practicum.ewm.model.Event;

import java.util.ArrayList;
import java.util.List;

@Repository
public class EventCustomRepository {

    public static Specification<Event> prepareSpecification(EventsFilter filter) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

//            var path = filter.getIsOwner()
//                    ? root.get("item").get("owner").get("id")
//                    : root.get("booker").get("id");
//            predicates.add(builder.equal(path, filter.getUserId()));
//
//            LocalDateTime now = LocalDateTime.now();
//            BфokingState state = filter.getState();
//
//            Predicate statePredicate = state.toPredicate(root, builder, now);
//
//            if (statePredicate != null) {
//                predicates.add(statePredicate);
//            }

            if (predicates.isEmpty()) {
                return builder.conjunction();
            }

            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}