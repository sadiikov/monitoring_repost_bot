package monitoringservicesreport.repository;

import monitoringservicesreport.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    // найти незакрытые инциденты для данного отсортированные по provider_name, start_time desc
    List<Incident> findByProviderNameAndStatusOrderByStartTimeDesc(String providerName, String status);
}
