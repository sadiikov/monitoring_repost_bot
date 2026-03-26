package monitoringservicesreport.bot;

import lombok.AllArgsConstructor;
import lombok.Getter;
import monitoringservicesreport.config.BotConfig;
import monitoringservicesreport.service.IncidentService;
import monitoringservicesreport.util.MessageParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@AllArgsConstructor
@Getter
public class StatusReporterBot extends TelegramLongPollingBot {
    private final Logger log = LoggerFactory.getLogger(StatusReporterBot.class);
    private final BotConfig cfg;
    private final IncidentService incidentService;

    @Override
    public String getBotUsername() { return cfg.botUsername; }

    @Override
    public String getBotToken() { return cfg.botToken; }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            // Проверяем, что апдейт действительно из канала и содержит текст
            if (!update.hasChannelPost() || update.getChannelPost().getText() == null) return;

            var msg = update.getChannelPost();
            String text = msg.getText();
            log.info("Получен пост из канала: {}, его чат = {}", text, msg.getChatId());

            // Парсим сообщение, чтобы понять, UP или DOWN
            MessageParser.ParsedMessage parsed = MessageParser.parse(text);
            if (parsed == null) {
                log.debug("Не удалось распарсить сообщение: {}", text);
                return;
            }

            // Время публикации поста (по таймзоне из настроек)
            Instant instant = Instant.ofEpochSecond(msg.getDate());
            LocalDateTime eventTime = LocalDateTime.ofInstant(
                    instant, ZoneId.of(cfg.timezone)
            ).withSecond(0).withNano(0);

            if (parsed.down) {
                incidentService.createDown(parsed.providerName, eventTime, text);
                log.info("DOWN сохранён: {} в {}", parsed.providerName, eventTime);
            } else if (parsed.up) {
                var closed = incidentService.closeIncident(parsed.providerName, eventTime);
                if (closed != null) {
                    log.info("UP применён: {} закрыт в {}, время простоя = {} мин",
                            parsed.providerName, eventTime, closed.getDowntimeMinutes());
                } else {
                    log.warn("UP получен, но открытый инцидент не найден для {}", parsed.providerName);
                }
            }

        } catch (Exception ex) {
            log.error("Ошибка при обработке поста из канала", ex);
        }
    }
}
