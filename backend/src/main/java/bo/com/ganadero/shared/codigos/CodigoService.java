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
        long numero = jdbc.sql("""
                insert into core.secuencias_codigo
                    (empresa_id,tipo_entidad,ambito_id,anio,ultimo_numero)
                values (:empresa,:tipo,:ambito,:anio,1)
                on conflict (empresa_id,tipo_entidad,ambito_id,anio)
                do update set ultimo_numero=core.secuencias_codigo.ultimo_numero+1,updated_at=now()
                returning ultimo_numero
                """)
                .param("empresa", user.empresaId())
                .param("tipo", tipo.name())
                .param("ambito", ambito)
                .param("anio", periodo)
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
                insert into core.secuencias_codigo
                    (empresa_id,tipo_entidad,ambito_id,anio,ultimo_numero)
                values (:empresa,:tipo,:ambito,:anio,:numero)
                on conflict (empresa_id,tipo_entidad,ambito_id,anio)
                do update set ultimo_numero=greatest(core.secuencias_codigo.ultimo_numero,excluded.ultimo_numero),
                              updated_at=now()
                """)
                .param("empresa", user.empresaId()).param("tipo", tipo.name())
                .param("ambito", ambitoId == null ? SIN_AMBITO : ambitoId)
                .param("anio", anio == null ? 0 : anio).param("numero", numero).update();
    }

    private long numeroCompatible(TipoCodigo tipo, int anio, String codigo) {
        String expresion = switch (tipo) {
            case PROPIEDAD -> "^PRP-(\\d+)$";
            case ANIMAL -> "^ANI-(\\d+)$";
            case LOTE -> "^LOT-" + anio + "-(\\d+)$";
            case SECTOR -> "^.+-SEC-(\\d+)$";
            case POTRERO -> "^.+-POT-(\\d+)$";
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
        };
    }

    private String codigoConPropiedad(UUID propiedadId, String tipo, long numero) {
        if (propiedadId == null) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        String propiedad = jdbc.sql("select codigo from core.propiedades where id=:id")
                .param("id", propiedadId).query(String.class).optional()
                .orElseThrow(() -> new BusinessException(ErrorCode.PROPERTY_NOT_FOUND));
        String sufijo = "-" + tipo + "-" + rellenar(numero, 3);
        String prefijo = normalizarManual(propiedad);
        if (prefijo.length() + sufijo.length() > 60) {
            prefijo = prefijo.substring(0, 60 - sufijo.length()).replaceAll("-+$", "");
        }
        return prefijo + sufijo;
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
