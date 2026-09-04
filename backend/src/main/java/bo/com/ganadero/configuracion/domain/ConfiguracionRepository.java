package bo.com.ganadero.configuracion.domain;
import java.util.UUID;

public interface ConfiguracionRepository {
 Configuracion get(); Configuracion update(Configuracion patch, UUID actor);
 void updatePin(String pinHash);
}
