package bo.com.ganadero.shared.codigos;

import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CodigoService {
    public static final String PERMISO_CODIGO_MANUAL = "CODIGO_MANUAL_ASIGNAR";
    private static final UUID SIN_AMBITO = new UUID(0, 0);
    private final JdbcClient jdbc;

    public CodigoService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String paraCreacion(CurrentUser user, TipoCodigo tipo, UUID ambitoId, Integer anio,
                               String codigoSolicitado) {
        if (tieneTexto(codigoSolicitado)) {
            exigirPermisoManual(user);
            String normalizado = normalizarManual(codigoSolicitado);
            sincronizarSecuenciaManual(user, tipo, ambitoId, anio, normalizado);
            return normalizado;
        }
        UUID ambito = ambitoId == null ? SIN_AMBITO : ambitoId;
        int periodo = anio == null ? 0 : anio;
        jdbc.sql("""
                insert into secuencia_codigo
                    (tipo_entidad,ambito_id,anio,ultimo_numero)
                values (:tipo,:ambito,:anio,1)
                on conflict (tipo_entidad,ambito_id,anio)
                do update set ultimo_numero=secuencia_codigo.ultimo_numero+1,updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now')
                """)
                .param("tipo", tipo.name())
                .param("ambito", ambito.toString())
                .param("anio", periodo)
                .update();
        long numero = jdbc.sql("select ultimo_numero from secuencia_codigo where tipo_entidad=:tipo and ambito_id=:ambito and anio=:anio")
                .param("tipo", tipo.name()).param("ambito", ambito.toString()).param("anio", periodo)
                .query(Long.class).single();
        return formatear(tipo, ambitoId, periodo, numero);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String paraActualizacion(CurrentUser user, TipoCodigo tipo, UUID ambitoId, Integer anio,
                                    String codigoActual, String codigoSolicitado) {
        if (!tieneTexto(codigoSolicitado)) return codigoActual;
        String normalizado = normalizarManual(codigoSolicitado);
        if (normalizado.equalsIgnoreCase(codigoActual)) return codigoActual;
        exigirPermisoManual(user);
        sincronizarSecuenciaManual(user, tipo, ambitoId, anio, normalizado);
        return normalizado;
    }

    private void sincronizarSecuenciaManual(CurrentUser user, TipoCodigo tipo, UUID ambitoId, Integer anio,
                                             String codigo) {
        long numero = numeroCompatible(tipo, anio == null ? 0 : anio, codigo);
        if (numero < 1) return;
        jdbc.sql("""
                insert into secuencia_codigo
                    (tipo_entidad,ambito_id,anio,ultimo_numero)
                values (:tipo,:ambito,:anio,:numero)
                on conflict (tipo_entidad,ambito_id,anio)
                do update set ultimo_numero=max(secuencia_codigo.ultimo_numero,excluded.ultimo_numero),
                              updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now')
                """)
                .param("tipo", tipo.name())
                .param("ambito", (ambitoId == null ? SIN_AMBITO : ambitoId).toString())
                .param("anio", anio == null ? 0 : anio).param("numero", numero).update();
    }

    private long numeroCompatible(TipoCodigo tipo, int anio, String codigo) {
        String expresion = switch (tipo) {
            case PROPIEDAD -> "^PRP-(\\d+)$";
            case ANIMAL -> "^ANI-(\\d+)$";
            case LOTE -> "^LOT-" + anio + "-(\\d+)$";
            case SECTOR -> "^.+-SEC-(\\d+)$";
            case POTRERO -> "^.+-POT-(\\d+)$";
            case COMPRA -> "^COM-(\\d+)$";
        };
        Matcher matcher = Pattern.compile(expresion).matcher(codigo);
        return matcher.matches() ? Long.parseLong(matcher.group(1)) : 0;
    }

    String formatear(TipoCodigo tipo, UUID ambitoId, int anio, long numero) {
        return switch (tipo) {
            case PROPIEDAD -> "PRP-" + rellenar(numero, 3);
            case ANIMAL -> "ANI-" + rellenar(numero, 6);
            case LOTE -> "LOT-" + anio + "-" + rellenar(numero, 4);
            case SECTOR -> codigoConPropiedad(ambitoId, "SEC", numero);
            case POTRERO -> codigoConPropiedad(ambitoId, "POT", numero);
            case COMPRA -> "COM-" + rellenar(numero, 6);
        };
    }

    /**
     * El contador en secuencia_codigo ya está aislado por ambito_id (propiedad), pero el texto
     * del código debe reflejar esa misma propiedad: de lo contrario, dos propiedades distintas
     * generan literalmente el mismo texto (p. ej. "FINCA-POT-001" en ambas) y chocan contra la
     * restricción UNIQUE global de la tabla.
     */
    private String codigoConPropiedad(UUID propiedadId, String tipo, long numero) {
        String sufijo = "-" + tipo + "-" + rellenar(numero, 3);
        String prefijo = propiedadId == null ? null
                : jdbc.sql("select codigo from propiedad where id=:id")
                        .param("id", propiedadId.toString())
                        .query(String.class).optional().orElse(null);
        return (prefijo == null ? "FINCA" : prefijo) + sufijo;
    }

    public String normalizarManual(String codigo) {
        String normalizado = codigo == null ? "" : codigo.trim().toUpperCase(Locale.ROOT)
                .replaceAll("\\s+", "-").replaceAll("-+", "-");
        if (normalizado.isBlank() || normalizado.length() > 60 || !normalizado.matches("[A-Z0-9-]+")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "El codigo solo puede contener letras, numeros y guiones, con un maximo de 60 caracteres.");
        }
        return normalizado;
    }

    private void exigirPermisoManual(CurrentUser user) {
        if (!user.hasPermission(PERMISO_CODIGO_MANUAL)) {
            throw new BusinessException(ErrorCode.USER_NOT_AUTHORIZED,
                    "El codigo se asigna automaticamente. Solo un administrador puede definirlo manualmente.");
        }
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static String rellenar(long numero, int ancho) {
        return String.format(Locale.ROOT, "%0" + ancho + "d", numero);
    }
}
