package ru.practicum.ewm.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.constant.EventSort;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class EventParamDto {
    private String text;

    private List<Long> categories;

    private Boolean paid;

    @JsonFormat(shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss",
            timezone = "Europe/Moscow")
    private LocalDateTime rangeStart;

    @JsonFormat(shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss",
            timezone = "Europe/Moscow")
    private LocalDateTime rangeEnd;

    private Boolean onlyAvailable;

    private EventSort sort;

    private Pageable pageable;
}
