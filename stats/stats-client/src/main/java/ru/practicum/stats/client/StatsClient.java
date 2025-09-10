package ru.practicum.stats.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.ewm.EndpointHitDto;
import ru.practicum.ewm.ViewStatsDto;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsClient {

    private final RestClient restClient;

    @Value("${stats-server.url}")
    private String statsServerUrl;

    public void saveHit(EndpointHitDto hitDto) {
        restClient.post()
                .uri(statsServerUrl + "/hit")
                .body(hitDto)
                .retrieve()
                .toBodilessEntity(); // ничего не возвращает
    }

    public List<ViewStatsDto> getStats(String start, String end, String[] uris, boolean unique) {

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(statsServerUrl + "/stats")
                .queryParam("start", URLEncoder.encode(start, StandardCharsets.UTF_8))
                .queryParam("end", URLEncoder.encode(end, StandardCharsets.UTF_8))
                .queryParam("uris", (Object[]) uris)
                .queryParam("unique", unique);

        return Arrays.asList(
                restClient.get()
                        .uri(builder.toUriString())
                        .retrieve()
                        .body(ViewStatsDto[].class)
        );
    }
}
