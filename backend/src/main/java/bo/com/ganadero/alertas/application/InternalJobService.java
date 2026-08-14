package bo.com.ganadero.alertas.application;

import bo.com.ganadero.shared.config.AppProperties;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.pesajes.application.ProcesarPesajesAtrasadosService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Ejecuta los jobs internos que invoca Supabase Cron en producción.
 * No dependen de un usuario autenticado: se autentican con un token de sistema
 * compartido (X-Ganadero-Cron-Secret) configurado en app.internal-jobs.secret.
 */
@Service
public class InternalJobService {
    private final AppProperties properties;
    private final ProcesadorAlertasProgramadasService procesador;
    private final ProcesarPesajesAtrasadosService pesajesAtrasados;

    public InternalJobService(AppProperties properties, ProcesadorAlertasProgramadasService procesador,
                              ProcesarPesajesAtrasadosService pesajesAtrasados) {
        this.properties = properties;
        this.procesador = procesador;
        this.pesajesAtrasados = pesajesAtrasados;
    }

    @Transactional
    public int activarAlertasVencidas(String suppliedToken) {
        validarAcceso(suppliedToken);
        return procesador.activarVencidas();
    }

    @Transactional
    public int procesarNotificacionesPendientes(String suppliedToken) {
        validarAcceso(suppliedToken);
        return procesador.procesarNotificacionesPendientes();
    }

    @Transactional
    public int generarAlertasPesajes(String suppliedToken) {
        validarAcceso(suppliedToken);
        return pesajesAtrasados.procesar();
    }

    private void validarAcceso(String supplied) {
        if (!properties.internalJobs().enabled()) throw new BusinessException(ErrorCode.INTERNAL_JOBS_DISABLED);
        byte[] expected = properties.internalJobs().secret() == null
                ? new byte[0] : properties.internalJobs().secret().getBytes(StandardCharsets.UTF_8);
        byte[] actual = supplied == null ? new byte[0] : supplied.getBytes(StandardCharsets.UTF_8);
        if (expected.length == 0 || !MessageDigest.isEqual(expected, actual))
            throw new BusinessException(ErrorCode.INTERNAL_JOBS_TOKEN_INVALID);
    }
}
