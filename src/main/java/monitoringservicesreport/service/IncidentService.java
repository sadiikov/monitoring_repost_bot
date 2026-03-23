package monitoringservicesreport.service;

import monitoringservicesreport.entity.Incident;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class IncidentService {

    private final GoogleSheetsService sheetsService;

    private final Map<String, Incident> activeIncidents = new ConcurrentHashMap<>();
    private final List<Incident> history = new CopyOnWriteArrayList<>();

    public IncidentService(GoogleSheetsService sheetsService) {
        this.sheetsService = sheetsService;
    }

    public Incident createDown(String providerName, LocalDateTime time, String sourceMessage) {

        Incident inc = new Incident();
        inc.setProviderName(providerName);
        inc.setStartTime(time);
        inc.setStatus("ACTIVE");
        inc.setSourceMessage(sourceMessage);
        inc.setCreatedAt(LocalDateTime.now());

        activeIncidents.put(providerName, inc);

        try {
            sheetsService.appendRow(Arrays.asList(
                    providerName,
                    time.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                    time.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
            ), time.toLocalDate());
        } catch (Exception e) {
            System.err.println("Ошибка при записи в Google Sheets:");
            e.printStackTrace();
        }

        return inc;
    }

    public Incident closeIncident(String providerName, LocalDateTime finishTime) {

        Incident inc = activeIncidents.remove(providerName);
        if (inc == null) return null;

        inc.setFinishTime(finishTime);
        inc.setStatus("CLOSED");

        LocalDateTime start = inc.getStartTime();
        Duration duration = Duration.between(start, finishTime);
        double value = duration.toMinutes() / 1440.0;

        try {
            if (start.toLocalDate().equals(finishTime.toLocalDate())) {

                sheetsService.updateRow(
                        inc.getProviderName(),
                        finishTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                        value,
                        start.toLocalDate()
                );

            } else {

                LocalDateTime midnight = start.toLocalDate().plusDays(1).atStartOfDay();

                Duration firstDuration = Duration.between(start, midnight);
                double firstValue = firstDuration.toMinutes() / 1440.0;

                Duration secondDuration = Duration.between(
                        finishTime.toLocalDate().atStartOfDay(),
                        finishTime
                );
                double secondValue = secondDuration.toMinutes() / 1440.0;

                // до полуночи
                sheetsService.updateRow(
                        inc.getProviderName(),
                        midnight.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                        firstValue,
                        start.toLocalDate()
                );

                // после полуночи
                sheetsService.appendRow(Arrays.asList(
                        inc.getProviderName(),
                        finishTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                        "00:00",
                        finishTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                        secondValue
                ), finishTime.toLocalDate());
            }

        } catch (Exception e) {
            System.err.println("Ошибка при обновлении Google Sheets:");
            e.printStackTrace();
        }

        inc.setDowntimeMinutes(value);

        history.add(inc);

        return inc;
    }
}