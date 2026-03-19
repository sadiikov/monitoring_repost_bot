package monitoringservicesreport.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageParser {

    private static final Pattern ID_LINE_PATTERN = Pattern.compile("(\\d{1,6})\\s*:\\s*(.+)");
    private static final Pattern UP_DOWN_PATTERN = Pattern.compile("(?i)\\b(ВКЛЮЧИЛ|ОТКЛЮЧИЛ)\\b");

    public static ParsedMessage parse(String text) {
        if (text == null) return null;
        String[] lines = text.split("\\r?\\n");
        boolean isUp = false, isDown = false;

        Matcher ud = UP_DOWN_PATTERN.matcher(text);
        if (ud.find()) {
            String m = ud.group(1).toLowerCase();
            if (m.contains("включ")) isUp = true;
            if (m.contains("отключ")) isDown = true;
        }

        // Найти строку где есть "число : название"
        for (String line : lines) {
            Matcher idm = ID_LINE_PATTERN.matcher(line);
            if (idm.find()) {
                String id = idm.group(1).trim();
                String name = idm.group(2).trim();
                ParsedMessage pm = new ParsedMessage();
                pm.providerId = id;
                pm.providerName = name;
                pm.up = isUp;
                pm.down = isDown;
                return pm;
            }
        }
        return null;
    }

    public static class ParsedMessage {
        public String providerId;
        public String providerName;
        public boolean up;
        public boolean down;
    }
}