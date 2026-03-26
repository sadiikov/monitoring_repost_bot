package monitoringservicesreport.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BotConfig {
    @Value("${bot.username}") public String botUsername;
    @Value("${bot.token}") public String botToken;
    @Value("${app.timezone:UTC}") public String timezone;
}