package bo.com.ganadero.timeline.domain;

/**
 * Catálogo estándar de tipos de evento de la línea de tiempo del animal (Bloque 25).
 *
 * <p>Cada tipo declara el módulo de origen y un título por defecto para
 * simplificar la publicación de eventos desde los módulos de negocio.</p>
 */
public enum TipoEventoAnimal {

    NACIMIENTO_REGISTRADO("ANIMALES", "Nacimiento registrado"),
    COMPRA_REGISTRADA("ANIMALES", "Compra registrada"),
    INGRESO_REGISTRADO("ANIMALES", "Ingreso registrado"),
    ANIMAL_ACTUALIZADO("ANIMALES", "Animal actualizado"),
    ESTADO_CAMBIADO("ANIMALES", "Estado cambiado"),

    IDENTIFICADOR_ASIGNADO("IDENTIFICADORES", "Identificador asignado"),
    IDENTIFICADOR_ACTUALIZADO("IDENTIFICADORES", "Identificador actualizado"),
    IDENTIFICADOR_RETIRADO("IDENTIFICADORES", "Identificador retirado"),
    IDENTIFICADOR_PRINCIPAL("IDENTIFICADORES", "Identificador principal"),
    IDENTIFICADOR_REEMPLAZADO("IDENTIFICADORES", "Identificador reemplazado"),
    QR_ASIGNADO("IDENTIFICADORES", "Código QR asignado"),
    QR_REEMPLAZADO("IDENTIFICADORES", "Código QR reemplazado"),

    GENEALOGIA_REGISTRADA("GENEALOGIA", "Genealogía registrada"),
    GENEALOGIA_ACTUALIZADA("GENEALOGIA", "Genealogía actualizada"),

    LOTE_ASIGNADO("LOTE", "Asignado a lote"),
    LOTE_CAMBIADO("LOTE", "Cambio de lote"),
    LOTE_REMOVIDO("LOTE", "Retirado del lote"),

    MOVIMIENTO_REGISTRADO("MOVIMIENTOS", "Movimiento registrado"),
    MOVIMIENTO_REVERTIDO("MOVIMIENTOS", "Movimiento revertido"),
    CUARENTENA_INICIADA("SANIDAD", "Cuarentena iniciada"),
    CUARENTENA_FINALIZADA("SANIDAD", "Cuarentena finalizada"),

    PESAJE_REGISTRADO("PESAJES", "Pesaje registrado"),
    PESAJE_ANULADO("PESAJES", "Pesaje anulado"),

    CELO_DETECTADO("REPRODUCCION", "Celo detectado"),
    SERVICIO_REGISTRADO("REPRODUCCION", "Servicio registrado"),
    DIAGNOSTICO_GESTACION_REGISTRADO("REPRODUCCION", "Diagnóstico de gestación registrado"),
    GESTACION_CONFIRMADA("REPRODUCCION", "Gestación confirmada"),
    GESTACION_DESCARTADA("REPRODUCCION", "Gestación descartada"),
    PERDIDA_GESTACION("REPRODUCCION", "Pérdida de gestación"),
    ABORTO_REGISTRADO("REPRODUCCION", "Aborto registrado"),
    PARTO_REGISTRADO("REPRODUCCION", "Parto registrado"),
    CRIA_REGISTRADA("REPRODUCCION", "Cría registrada"),
    DESTETE_REGISTRADO("REPRODUCCION", "Destete registrado"),

    VACUNACION_APLICADA("SANIDAD", "Vacunación aplicada"),
    JORNADA_SANITARIA_CONFIRMADA("SANIDAD", "Jornada sanitaria confirmada"),
    CASO_CLINICO_ABIERTO("SANIDAD", "Caso clínico abierto"),
    CASO_CLINICO_CERRADO("SANIDAD", "Caso clínico cerrado"),
    TRATAMIENTO_INICIADO("SANIDAD", "Tratamiento iniciado"),
    TRATAMIENTO_APLICADO("SANIDAD", "Tratamiento aplicado"),
    TRATAMIENTO_FINALIZADO("SANIDAD", "Tratamiento finalizado"),

    FOTO_AGREGADA("ARCHIVOS", "Fotografía agregada"),
    FOTO_PRINCIPAL_CAMBIADA("ARCHIVOS", "Fotografía principal"),
    FOTO_ELIMINADA("ARCHIVOS", "Fotografía eliminada"),
    ORIGEN_SYNC("SYNC", "Cambio originado por sincronización");

    private final String modulo;
    private final String titulo;

    TipoEventoAnimal(String modulo, String titulo) {
        this.modulo = modulo;
        this.titulo = titulo;
    }

    public String modulo() {
        return modulo;
    }

    public String titulo() {
        return titulo;
    }
}
