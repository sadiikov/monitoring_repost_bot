package monitoringservicesreport.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class Incident {
    private Long id;

    private String providerName;

    private LocalDateTime startTime;

    private LocalDateTime finishTime;

    private Integer downtimeMinutes;

    private String status;

    private String sourceMessage;

    private LocalDateTime createdAt;
}
