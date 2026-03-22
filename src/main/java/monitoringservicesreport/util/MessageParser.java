package monitoringservicesreport.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageParser {

    // 1. 10597 : Mikrokreditbank
    private static final Pattern ID_LINE_PATTERN =
            Pattern.compile("^(\\d{1,6})\\s*:\\s*(.+)$");

    // 2. ВКЛЮЧИЛ / ОТКЛЮЧИЛ
    private static final Pattern UP_DOWN_PATTERN =
            Pattern.compile("(?i)\\b(ВКЛЮЧИЛ|ОТКЛЮЧИЛ)\\b");

    // 3. банк-клиента 'XXX' изменен на АКТИВНЫЙ/НЕАКТИВНЫЙ
    private static final Pattern BANK_PATTERN =
            Pattern.compile("банк-клиента\\s+'(.+?)'\\s+изменен на\\s+(АКТИВНЫЙ|НЕАКТИВНЫЙ)",
                    Pattern.CASE_INSENSITIVE);

    // 4. Дата + время
    private static final Pattern DATE_TIME_PATTERN =
            Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}:\\d{2})");

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    public static ParsedMessage parse(String text) {
        if (text == null || text.isBlank()) return null;

        String[] lines = text.split("\\r?\\n");

        boolean isUp = false;
        boolean isDown = false;

        // -------------------------------
        // 1. Парсим дату/время
        // -------------------------------
        LocalDateTime dateTime = null;
        Matcher dt = DATE_TIME_PATTERN.matcher(text);
        if (dt.find()) {
            dateTime = LocalDateTime.parse(dt.group(1), DATE_TIME_FORMAT);
        }

        // -------------------------------
        // 2. Проверка ВКЛЮЧИЛ / ОТКЛЮЧИЛ
        // -------------------------------
        Matcher ud = UP_DOWN_PATTERN.matcher(text);
        if (ud.find()) {
            String m = ud.group(1).toLowerCase();
            if (m.contains("включ")) isUp = true;
            if (m.contains("отключ")) isDown = true;
        }

        // -------------------------------
        // 3. Формат: банк-клиента
        // -------------------------------
        Matcher bankMatcher = BANK_PATTERN.matcher(text);
        if (bankMatcher.find()) {
            ParsedMessage pm = new ParsedMessage();
            pm.providerName = bankMatcher.group(1).trim();
            pm.providerId = null;

            String status = bankMatcher.group(2).toUpperCase();

            if ("АКТИВНЫЙ".equals(status)) {
                pm.up = true;
            } else if ("НЕАКТИВНЫЙ".equals(status)) {
                pm.down = true;
            }

            pm.dateTime = dateTime;
            return pm;
        }

        // -------------------------------
        // 4. Формат: ID : NAME
        // -------------------------------
        for (String line : lines) {
            line = line.trim();

            // пропускаем строки с датой
            if (DATE_TIME_PATTERN.matcher(line).find()) continue;

            Matcher idm = ID_LINE_PATTERN.matcher(line);
            if (idm.matches()) { // <-- ВАЖНО matches()
                ParsedMessage pm = new ParsedMessage();
                pm.providerId = idm.group(1).trim();
                pm.providerName = idm.group(2).trim();
                pm.up = isUp;
                pm.down = isDown;
                pm.dateTime = dateTime;
                return pm;
            }
        }

        // -------------------------------
        // 5. Формат: просто название (после "провайдера")
        // -------------------------------
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim().toLowerCase();

            if (line.contains("провайдера")) {
                for (int j = i + 1; j < lines.length; j++) {
                    String next = lines[j].trim();

                    if (next.isEmpty()) continue;

                    // если дошли до даты — стоп
                    if (DATE_TIME_PATTERN.matcher(next).find()) break;

                    ParsedMessage pm = new ParsedMessage();
                    pm.providerName = next;
                    pm.providerId = null;
                    pm.up = isUp;
                    pm.down = isDown;
                    pm.dateTime = dateTime;
                    return pm;
                }
            }
        }

        return null;
    }

    public static class ParsedMessage {
        public String providerId;
        public String providerName;
        public boolean up;
        public boolean down;
        public LocalDateTime dateTime;

        @Override
        public String toString() {
            return "ParsedMessage{" +
                    "providerId='" + providerId + '\'' +
                    ", providerName='" + providerName + '\'' +
                    ", up=" + up +
                    ", down=" + down +
                    ", dateTime=" + dateTime +
                    '}';
        }
    }
}