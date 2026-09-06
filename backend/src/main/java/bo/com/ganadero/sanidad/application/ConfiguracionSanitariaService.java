package bo.com.ganadero.sanidad.application;

import bo.com.ganadero.animales.domain.*;
import bo.com.ganadero.sanidad.domain.ResultadoExamenReproductivo;
import bo.com.ganadero.shared.security.*;
import bo.com.ganadero.shared.error.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;

@Service
public class ConfiguracionSanitariaService {
    public record Configuracion(Integer edadMinMachoMeses, Integer edadMinHembraMeses,
                               Integer horizonteProyeccionMeses, long version) {}
    private final JdbcClient jdbc;
    private final UserContext context;
    private final ApplicationEventPublisher events;
    public ConfiguracionSanitariaService(JdbcClient jdbc, UserContext context, ApplicationEventPublisher events) {
        this.jdbc = jdbc; this.context = context; this.events = events;
    }
    private String ambito(CurrentUser u) { return u.empresaId() == null ? "LOCAL" : u.empresaId().toString(); }
    private Configuracion leer(CurrentUser u) {
        return jdbc.sql("select * from configuracion_sanitaria where ambito=:a").param("a", ambito(u))
                .query((r,n) -> new Configuracion((Integer)r.getObject("edad_min_macho_meses"),
                        (Integer)r.getObject("edad_min_hembra_meses"),
                        (Integer)r.getObject("horizonte_proyeccion_meses"),r.getLong("version")))
                .optional().orElse(new Configuracion(null,null,12,0));
    }
    @Transactional(readOnly=true)
    public Configuracion consultar() { return leer(context.requirePermission("SANIDAD_VER")); }

    @Transactional
    public Configuracion guardar(Configuracion c) {
        CurrentUser u=context.requirePermission("SANIDAD_PLAN_ADMINISTRAR");
        ReglasSanitarias.exigir(c.edadMinMachoMeses()!=null && c.edadMinMachoMeses()>=1 && c.edadMinMachoMeses()<=120
                && c.edadMinHembraMeses()!=null && c.edadMinHembraMeses()>=1 && c.edadMinHembraMeses()<=120,
                "Configure las dos edades mínimas en meses enteros, entre 1 y 120.");
        ReglasSanitarias.exigir(c.horizonteProyeccionMeses()!=null && c.horizonteProyeccionMeses()>=1
                && c.horizonteProyeccionMeses()<=24, "El horizonte de proyección del calendario debe estar entre 1 y 24 meses.");
        jdbc.sql("insert into configuracion_sanitaria(ambito) values(:a) on conflict do nothing").param("a",ambito(u)).update();
        int n=jdbc.sql("update configuracion_sanitaria set edad_min_macho_meses=:m,edad_min_hembra_meses=:h,horizonte_proyeccion_meses=:hp,version=version+1,actualizado_por=:u,actualizado_en=:f where ambito=:a and version=:v")
                .param("m",c.edadMinMachoMeses()).param("h",c.edadMinHembraMeses()).param("hp",c.horizonteProyeccionMeses())
                .param("u",u.userId().toString())
                .param("f",Instant.now().toString()).param("a",ambito(u)).param("v",c.version()).update();
        if(n!=1) throw new BusinessException(ErrorCode.VERSION_CONFLICT);
        events.publishEvent(new SanidadAuditEvent(u.empresaId(),u.userId(),"CONFIGURAR_EDADES_EXAMEN","CONFIGURACION_SANITARIA",u.userId(),Instant.now()));
        return leer(u);
    }

    public void validarExamen(Animal animal, CrearExamenReproductivoCommand c) {
        Configuracion config=leer(context.requirePermission("SANIDAD_EXAMEN_REPRODUCTIVO_CREAR"));
        Integer meses=animal.sexo()==SexoAnimal.MACHO?config.edadMinMachoMeses():config.edadMinHembraMeses();
        ReglasSanitarias.exigir(meses!=null,"Configure primero las edades mínimas en Sanidad → Planes sanitarios.");
        ReglasSanitarias.exigir(animal.fechaNacimiento()!=null,"Registre la fecha de nacimiento del animal para comprobar su edad.");
        ReglasSanitarias.exigir(!c.fecha().isBefore(animal.fechaNacimiento().plusMonths(meses)),
                "El animal no alcanza la edad mínima configurada de "+meses+" meses en la fecha del examen.");
        porcentaje(c.motilidadEspermaticaPct()); porcentaje(c.morfologiaPct()); porcentaje(c.porcentajePesoAdulto());
        positivo(c.pesoKg()); positivo(c.circunferenciaEscrotalCm());
        ReglasSanitarias.exigir(c.condicionCorporal()==null || (c.condicionCorporal().compareTo(BigDecimal.ONE)>=0 && c.condicionCorporal().compareTo(BigDecimal.valueOf(5))<=0),"La condición corporal debe estar entre 1 y 5.");
        if(c.resultado()==ResultadoExamenReproductivo.APTO) {
            ReglasSanitarias.exigir(c.observaciones()!=null && !c.observaciones().isBlank(),
                    "Para registrar APTO documente la conclusión de la evaluación en observaciones. La edad por sí sola no acredita aptitud.");
            boolean evaluado=animal.sexo()==SexoAnimal.MACHO
                    ? c.circunferenciaEscrotalCm()!=null && c.motilidadEspermaticaPct()!=null && c.morfologiaPct()!=null
                    : c.pesoKg()!=null && c.condicionCorporal()!=null && c.desarrolloReproductivo()!=null && !c.desarrolloReproductivo().isBlank();
            ReglasSanitarias.exigir(evaluado,"Para registrar APTO complete la evaluación física y reproductiva correspondiente al sexo del animal.");
        }
    }
    private void porcentaje(BigDecimal n) { ReglasSanitarias.exigir(n==null || (n.signum()>=0 && n.compareTo(BigDecimal.valueOf(100))<=0),"Los porcentajes deben estar entre 0 y 100."); }
    private void positivo(BigDecimal n) { ReglasSanitarias.exigir(n==null || n.signum()>0,"El peso y la circunferencia deben ser mayores a cero."); }
}
