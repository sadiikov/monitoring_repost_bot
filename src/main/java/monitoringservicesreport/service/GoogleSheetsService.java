package monitoringservicesreport.service;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
import monitoringservicesreport.config.AppConfig;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class GoogleSheetsService {

    private final Sheets sheets;
    private final String spreadsheetId;

    public GoogleSheetsService(Sheets sheets, AppConfig cfg) {
        this.sheets = sheets;
        this.spreadsheetId = cfg.getSpreadsheetId();
    }

    private String getSheetNameForDate(LocalDate date) {
        return date.withDayOfMonth(1).format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
    }

    private void ensureSheetExists(String sheetName) throws IOException {
        Spreadsheet spreadsheet = sheets.spreadsheets().get(spreadsheetId).execute();

        boolean exists = spreadsheet.getSheets().stream()
                .map(Sheet::getProperties)
                .anyMatch(p -> p.getTitle().equals(sheetName));

        if (!exists) {
            int sheetId = (int) (Math.random() * 1_000_000);

            AddSheetRequest addSheetRequest = new AddSheetRequest()
                    .setProperties(new SheetProperties()
                            .setTitle(sheetName)
                            .setSheetId(sheetId));

            RepeatCellRequest formatColumnE = new RepeatCellRequest()
                    .setRange(new GridRange()
                            .setSheetId(sheetId)
                            .setStartRowIndex(1)
                            .setStartColumnIndex(4)
                            .setEndColumnIndex(5))
                    .setCell(new CellData()
                            .setUserEnteredFormat(new CellFormat()
                                    .setNumberFormat(new NumberFormat()
                                            .setType("TIME")
                                            .setPattern("hh:mm"))))
                    .setFields("userEnteredFormat.numberFormat");

            BatchUpdateSpreadsheetRequest batchRequest = new BatchUpdateSpreadsheetRequest()
                    .setRequests(List.of(
                            new Request().setAddSheet(addSheetRequest),
                            new Request().setRepeatCell(formatColumnE)
                    ));

            sheets.spreadsheets().batchUpdate(spreadsheetId, batchRequest).execute();

            ValueRange headers = new ValueRange().setValues(List.of(List.of(
                    "Наименование провайдера", "Дата", "Старт", "Финиш", "Время простоя"
            )));

            sheets.spreadsheets().values()
                    .append(spreadsheetId, sheetName + "!A:E", headers)
                    .setValueInputOption("RAW")
                    .execute();
        }
    }

    public void appendRow(List<Object> values, LocalDate date) throws IOException {
        String sheetName = getSheetNameForDate(date);
        ensureSheetExists(sheetName);

        // Читаем только A:E
        ValueRange response = sheets.spreadsheets().values()
                .get(spreadsheetId, sheetName + "!A:E")
                .execute();

        List<List<Object>> rows = response.getValues();
        int nextRow = 1; // если таблица пустая, то первая строка
        if (rows != null) {
            // Находим первую пустую строку среди A:E
            nextRow = rows.size() + 1;
            for (int i = 0; i < rows.size(); i++) {
                List<Object> row = rows.get(i);
                boolean empty = row.stream().allMatch(v -> v.toString().isBlank());
                if (empty) {
                    nextRow = i + 1; // строки в API нумеруются с 1
                    break;
                }
            }
        }

        // Пишем ровно в найденную строку
        List<Object> trimmed = new ArrayList<>(values.subList(0, Math.min(values.size(), 5)));
        String range = sheetName + "!A" + nextRow + ":E" + nextRow;

        ValueRange body = new ValueRange().setValues(Collections.singletonList(trimmed));
        sheets.spreadsheets().values()
                .update(spreadsheetId, range, body)
                .setValueInputOption("RAW")
                .execute();
    }


    public void updateRow(String providerName, String finish, double downtime, LocalDate date) throws IOException {
        String sheetName = getSheetNameForDate(date);
        ensureSheetExists(sheetName);

        ValueRange response = sheets.spreadsheets().values()
                .get(spreadsheetId, sheetName + "!A:E")
                .execute();

        List<List<Object>> rows = response.getValues();
        if (rows == null) return;

        for (int i = 0; i < rows.size(); i++) {
            List<Object> row = rows.get(i);

            if (!row.isEmpty() && row.get(0).toString().equals(providerName)
                    && (row.size() < 4 || row.get(3).toString().isBlank())) {

                String dateStr = row.size() > 1 ? row.get(1).toString() : "";
                String start = row.size() > 2 ? row.get(2).toString() : "";

                List<Object> updated = Arrays.asList(providerName, dateStr, start, finish, downtime);
                String range = sheetName + "!A" + (i + 1) + ":E" + (i + 1);
                ValueRange body = new ValueRange().setValues(Collections.singletonList(updated));

                sheets.spreadsheets().values()
                        .update(spreadsheetId, range, body)
                        .setValueInputOption("RAW")
                        .execute();
                break;
            }
        }
    }
}