package bo.com.ganadero.integraciones.telegram;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${ganadero.firebase.credentials-path:}")
    private String customCredentialsPath;

    @Bean
    public Optional<Firestore> firestoreClient() {
        try {
            File credentialsFile = resolveCredentialsFile();
            if (credentialsFile == null || !credentialsFile.exists() || !credentialsFile.canRead()) {
                log.info("Firebase: No se encontró archivo de credenciales. El Relay Cloud funcionará en modo local/desconectado hasta que se configure.");
                return Optional.empty();
            }

            try (InputStream serviceAccount = new FileInputStream(credentialsFile)) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp app;
                if (FirebaseApp.getApps().isEmpty()) {
                    app = FirebaseApp.initializeApp(options, "ganadero-relay");
                } else {
                    app = FirebaseApp.getInstance("ganadero-relay");
                }

                Firestore firestore = FirestoreClient.getFirestore(app);
                log.info("Firebase: Conexión establecida con éxito con Firestore Relay desde {}", credentialsFile.getAbsolutePath());
                return Optional.of(firestore);
            }
        } catch (Exception e) {
            log.warn("Firebase: Error al inicializar Firebase Admin SDK ({}). La sincronización Cloud queda pausada.", e.getMessage());
            return Optional.empty();
        }
    }

    private File resolveCredentialsFile() {
        if (customCredentialsPath != null && !customCredentialsPath.isBlank()) {
            File f = new File(customCredentialsPath);
            if (f.exists()) return f;
        }

        String envPath = System.getenv("FIREBASE_CREDENTIALS_PATH");
        if (envPath != null && !envPath.isBlank()) {
            File f = new File(envPath);
            if (f.exists()) return f;
        }

        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            Path p = Paths.get(appData, "Ganadero", "credentials", "firebase-service-account.json");
            if (p.toFile().exists()) return p.toFile();
        }

        String userHome = System.getProperty("user.home");
        if (userHome != null && !userHome.isBlank()) {
            Path p = Paths.get(userHome, ".ganadero", "credentials", "firebase-service-account.json");
            if (p.toFile().exists()) return p.toFile();
        }

        File localRel = new File("credentials/firebase-service-account.json");
        if (localRel.exists()) return localRel;

        File localRoot = new File("firebase-service-account.json");
        if (localRoot.exists()) return localRoot;

        return null;
    }
}
