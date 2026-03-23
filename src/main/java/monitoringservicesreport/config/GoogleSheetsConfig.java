package monitoringservicesreport.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.List;

@Configuration
public class GoogleSheetsConfig {

    private static final String APPLICATION_NAME = "Monitoring Services Report";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String LOCAL_CREDENTIALS = "src/main/resources/google-sheets-credentials.json";

    @Bean
    public Sheets sheetsService() throws GeneralSecurityException, IOException {
        var httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        String path = System.getenv("GOOGLE_CREDS_PATH");

        InputStream credentialsStream;

        if(path != null && !path.isBlank()) {
            credentialsStream = new FileInputStream(path);
        }else{
            credentialsStream = new FileInputStream(LOCAL_CREDENTIALS);
        }

        GoogleCredential credential = GoogleCredential.fromStream(credentialsStream)
                .createScoped(List.of("https://www.googleapis.com/auth/spreadsheets"));

        return new Sheets.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}
