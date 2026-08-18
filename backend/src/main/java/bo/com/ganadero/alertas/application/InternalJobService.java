package bo.com.ganadero.alertas.application;

import bo.com.ganadero.shared.config.AppProperties;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.pesajes.application.ProcesarPesajesAtrasadosService;
import bo.com.ganadero.sanidad.application.ProcesarAlertasVacunacionService;
import bo.com.ganadero.sanidad.application.ProcesarTratamientosVencidosService;
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
    private final ProcesarAlertasVacunacionService alertasVacunacion;
    private final ProcesarTratamientosVencidosService tratamientosVencidos;
    private final RecordatorioService recordatorios;

    public InternalJobService(AppProperties properties, ProcesadorAlertasProgramadasService procesador,
                              ProcesarPesajesAtrasadosService pesajesAtrasados,
                              ProcesarAlertasVacunacionService alertasVacunacion,
                              ProcesarTratamientosVencidosService tratamientosVencidos,
                              RecordatorioService recordatorios) {
        this.properties = properties;
        this.procesador = procesador;
        this.pesajesAtrasados = pesajesAtrasados;
        this.alertasVacunacion = alertasVacunacion;
        this.tratamientosVencidos = tratamientosVencidos;
        this.recordatorios = recordatorios;
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

    @Transactional
    public int generarAlertasVacunacion(String suppliedToken) {
        validarAcceso(suppliedToken);
        return alertasVacunacion.procesar();
    }

    @Transactional
    public int procesarTratamientosVencidos(String suppliedToken) {
        validarAcceso(suppliedToken);
        return tratamientosVencidos.procesar();
    }

    @Transactional
    public int procesarRecordatorios(String suppliedToken) {
        validarAcceso(suppliedToken);
        return recordatorios.procesar();
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
