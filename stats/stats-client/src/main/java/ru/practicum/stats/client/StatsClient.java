package ru.practicum.stats.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsClient {

    private final RestClient restClient;

    @Value("${stats-server.url}")
    private String statsServerUrl;

    public void saveHit(EndpointHitDto hitDto) {
        String url = statsServerUrl + "/hit";
        log.info("Отправка запроса saveHit: url={}, body={}", url, hitDto);

        restClient.post()
                .uri(statsServerUrl + "/hit")
                .body(hitDto)
                .retrieve()
                .toBodilessEntity();

        log.info("Hit был сохранен");
    }

    public List<ViewStatsDto> getStats(String start, String end, String[] uris, boolean unique) {

        String url = UriComponentsBuilder
                .fromHttpUrl(statsServerUrl + "/stats")
                .queryParam("start", URLEncoder.encode(start, StandardCharsets.UTF_8))
                .queryParam("end", URLEncoder.encode(end, StandardCharsets.UTF_8))
                .queryParam("uris", (Object[]) uris)
                .queryParam("unique", unique)
                .toUriString();

        log.info("Отправка запроса getStats: url={}", url);

        List<ViewStatsDto> stats = Arrays.asList(
                restClient.get()
                        .uri(url)
                        .retrieve()
                        .body(ViewStatsDto[].class)
        );

        log.info("getStats вернул {} записей", stats.size());
        return stats;
    }
}
