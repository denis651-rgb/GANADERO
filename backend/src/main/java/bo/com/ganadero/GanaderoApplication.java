package bo.com.ganadero;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GanaderoApplication {

    public static void main(String[] args) {
        SpringApplication.run(GanaderoApplication.class, args);
    }
}
