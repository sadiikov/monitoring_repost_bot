package monitoringservicesreport.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "incident")
@Getter
@Setter
@NoArgsConstructor
public class Incident {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_name")
    private String providerName;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "finish_time")
    private LocalDateTime finishTime;

    @Column(name = "downtime_minutes")
    private Integer downtimeMinutes;

    @Column(name = "status")
    private String status;

    @Column(name = "source_message", columnDefinition = "text")
    private String sourceMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
