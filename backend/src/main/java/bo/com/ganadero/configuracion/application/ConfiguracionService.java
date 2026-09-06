package bo.com.ganadero.configuracion.application;
import bo.com.ganadero.configuracion.domain.*; import bo.com.ganadero.shared.security.*;
import org.springframework.context.ApplicationEventPublisher; import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.time.Instant; import java.util.*;

/** Ajustes globales de la app de escritorio (moneda, unidades, dias de alerta, PIN). */
@Service public class ConfiguracionService {
 private static final BCryptPasswordEncoder PIN_ENCODER=new BCryptPasswordEncoder();
 private final ConfiguracionRepository repo; private final UserContext context; private final ApplicationEventPublisher events;
 public ConfiguracionService(ConfiguracionRepository r,UserContext c,ApplicationEventPublisher e){repo=r;context=c;events=e;}
 @Transactional(readOnly=true) public Configuracion get(){context.requirePermission("CONFIGURACION_VER");return repo.get();}
 @Transactional public Configuracion update(ConfiguracionCommand c){
  CurrentUser u=context.requirePermission("CONFIGURACION_EDITAR");
  Configuracion patch=new Configuracion(null,c.zonaHoraria(),c.moneda(),c.unidadPeso(),c.unidadSuperficie(),
   c.diasAlertaPreparto(),c.diasAlertaVacunacion(),c.diasSinPesaje(),c.diasAlertaDestete(),
   c.diasDiagnosticoPostServicio(),c.diasGestacionEstimada(),c.comprimirImagenes(),c.calidadImagen(),c.nombreUsuario(),
   false,c.diasToleranciaPesoCompra(),Objects.requireNonNull(c.version()));
  Configuracion saved=repo.update(patch,u.userId());
  if(c.nuevoPin()!=null&&!c.nuevoPin().isBlank())repo.updatePin(PIN_ENCODER.encode(c.nuevoPin()));
  else if(Boolean.TRUE.equals(c.quitarPin()))repo.updatePin(null);
  events.publishEvent(new ConfiguracionAuditEvent(u.empresaId(),u.userId(),Instant.now()));
  return repo.get();
 }
}
