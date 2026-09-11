package bo.com.ganadero.respaldos.domain;

public enum EstadoRespaldo {
    CREANDO,
    CREADO_LOCALMENTE,
    COPIANDO_A_CARPETA_EXTERNA,
    COPIADO_A_CARPETA_EXTERNA,
    ERROR_DE_COPIA,
    INTEGRIDAD_INVALIDA
}
