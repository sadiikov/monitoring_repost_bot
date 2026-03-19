package monitoringservicesreport.config;

import monitoringservicesreport.bot.StatusReporterBot;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class BotInitializer {

    public BotInitializer(StatusReporterBot bot) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(bot);
            System.out.println("✅ Bot successfully registered!");
        } catch (Exception e) {
            System.err.println("❌ Failed to register bot: " + e.getMessage());
        }
    }
}
