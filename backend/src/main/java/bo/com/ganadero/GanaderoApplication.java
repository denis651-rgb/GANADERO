package bo.com.ganadero;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class GanaderoApplication {

    public static void main(String[] args) {
        ensureDatabaseDirectoryExists();
        SpringApplication.run(GanaderoApplication.class, args);
    }

    // sqlite-jdbc no crea el directorio del archivo por si solo (a diferencia de LocalFileStorageClient
    // para media); sin esto, el primer arranque en una maquina limpia falla antes de que Flyway conecte.
    private static void ensureDatabaseDirectoryExists() {
        String dbPath = System.getenv().getOrDefault("GANADERO_DB_PATH", "./data/ganadero.db");
        Path parent = Path.of(dbPath).toAbsolutePath().normalize().getParent();
        if (parent == null) return;
        try {
            Files.createDirectories(parent);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo crear el directorio de la base de datos local: " + parent, exception);
        }
    }
}
