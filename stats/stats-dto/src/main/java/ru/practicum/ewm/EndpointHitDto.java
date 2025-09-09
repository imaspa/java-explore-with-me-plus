package ru.practicum.ewm;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class EndpointHitDto {
    @NotBlank
    private String app;
    @NotBlank
    private String uri;
    @NotBlank
    @Pattern(regexp = "^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$|^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$",
            message = "Некорректный IP адрес")
    private String ip;
    @NotBlank
    @JsonFormat(shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss",
            timezone = "Europe/Moscow")
    private LocalDateTime timestamp;
}
