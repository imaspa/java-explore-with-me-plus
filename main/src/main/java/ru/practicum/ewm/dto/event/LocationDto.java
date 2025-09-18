package ru.practicum.ewm.dto.event;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationDto {
    @NotBlank
    private Float lat;

    @NotBlank
    private Float lon;
}
