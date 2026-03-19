package monitoringservicesreport.service;

import monitoringservicesreport.entity.Incident;
import monitoringservicesreport.repository.IncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class IncidentService {

    private final IncidentRepository repo;
    private final GoogleSheetsService sheetsService;

    public IncidentService(IncidentRepository repo, GoogleSheetsService sheetsService) {
        this.repo = repo;
        this.sheetsService = sheetsService;
    }

    @Transactional
    public Incident createDown(String providerName, LocalDateTime time, String sourceMessage) {
        Incident inc = new Incident();
        inc.setProviderName(providerName);
        inc.setStartTime(time);
        inc.setStatus("ACTIVE");
        inc.setSourceMessage(sourceMessage);
        inc.setCreatedAt(LocalDateTime.now());

        Incident saved = repo.save(inc);

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

        return saved;
    }

    @Transactional
    public Incident closeIncident(String providerName, LocalDateTime finishTime) {
        var openList = repo.findByProviderNameAndStatusOrderByStartTimeDesc(providerName, "ACTIVE");
        if (openList.isEmpty()) return null;

        Incident inc = openList.get(0);
        inc.setFinishTime(finishTime);
        inc.setStatus("CLOSED");

        // Проверяем, пересекла ли авария полночь
        LocalDateTime start = inc.getStartTime();
        long totalMinutes = Duration.between(start, finishTime).toMinutes();

        try {
            if (start.toLocalDate().equals(finishTime.toLocalDate())) {
                // Один день
                sheetsService.updateRow(inc.getProviderName(),
                        finishTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                        totalMinutes + " мин",
                        start.toLocalDate());
            } else {
                // Разбиваем на 2 записи
                LocalDateTime midnight = start.toLocalDate().atTime(LocalTime.MAX);
                long firstPart = Duration.between(start, midnight).toMinutes();
                long secondPart = Duration.between(finishTime.toLocalDate().atStartOfDay(), finishTime).toMinutes();

                // Первая часть (до полуночи)
                sheetsService.updateRow(inc.getProviderName(),
                        midnight.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                        firstPart + " мин",
                        start.toLocalDate());

                // Вторая часть (после полуночи)
                sheetsService.appendRow(Arrays.asList(
                        inc.getProviderName(),
                        finishTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                        "00:00",
                        finishTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                        secondPart + " мин"
                ), finishTime.toLocalDate());
            }
        } catch (Exception e) {
            System.err.println("Ошибка при обновлении Google Sheets:");
            e.printStackTrace();
        }

        inc.setDowntimeMinutes((int) totalMinutes);
        return repo.save(inc);
    }
}