# Arquitectura de Fase 3

Esta decisión arquitectónica es normativa para Reproducción, Sanidad, Alertas y notificaciones.

## Responsabilidades

| Componente | Responsabilidad |
|---|---|
| Spring Boot | Decide qué ocurrió, valida las reglas ganaderas, calcula fechas y crea, cancela o resuelve alertas. |
| Supabase PostgreSQL | Persiste los datos y estados decididos por Spring Boot. |
| Supabase Cron | Determina cuándo corresponde procesar notificaciones cuya fecha programada ya venció. No calcula fechas ganaderas. |
| Supabase Edge Function | Procesa notificaciones pendientes y envía Web Push. No decide reglas de negocio. |
| PWA | Recibe y muestra la notificación. |

## Flujo obligatorio

Los módulos Reproducción y Sanidad producen eventos y fechas futuras. Spring Boot los convierte en alertas `PROGRAMADA` dentro de `alertas.alertas`. Supabase Cron selecciona las alertas vencidas; una Edge Function crea o procesa `alertas.notificaciones` y envía Web Push a la PWA.

Tipos previstos: `PROXIMO_PARTO`, `DIAGNOSTICO_GESTACION_PENDIENTE`, `DESTETE_PROXIMO`, `VACUNACION_PROXIMA`, `TRATAMIENTO_PENDIENTE`, `TRATAMIENTO_ATRASADO`, `RETIRO_SANITARIO` y `CUARENTENA_POR_FINALIZAR`.

## Reproducción 3.1

La fecha recomendada de diagnóstico la calcula Spring Boot desde la fecha del servicio. El intervalo se configura con `ganadero.reproduccion.dias-hasta-diagnostico` y su valor predeterminado es 28 días. Un diagnóstico positivo calcula la fecha probable de parto con 285 días de gestación. La creación efectiva de alertas queda conectada al Motor de Alertas de la Fase 3.6; Cron nunca replica estos cálculos.

En Fase 3.2, el parto, sus crías, los animales creados, la genealogía y los pesajes de nacimiento se guardan en una sola transacción. Al crear una cría viva, Spring calcula la fecha sugerida de destete mediante `ganadero.reproduccion.dias-hasta-destete` (210 días por defecto) y programa `DESTETE_PROXIMO` cuando el Motor de Alertas está disponible. Partos y abortos resuelven `PROXIMO_PARTO`; el destete resuelve `DESTETE_PROXIMO`.

## Plan sanitario 3.3

Sanidad mantiene enfermedades globales y específicas de empresa, planes e ítems. Las enfermedades históricas se desactivan y no se eliminan. Solo los planes `ACTIVO` pueden producir próximas actividades.

La integración futura con Inventario se realiza exclusivamente mediante `ProductoSanitarioPort`; Sanidad no consulta tablas internas de Inventario. Después de una aplicación, Spring calcula `próximaAplicación = fechaAplicación + frecuenciaDías` y `fechaAlerta = próximaAplicación - díasAlerta`. No se materializan anticipadamente aplicaciones para todos los animales y Supabase Cron no repite el cálculo.

## Jornadas sanitarias 3.4

La selección masiva se guarda en la jornada y se confirma mediante una única operación transaccional. `operationId` identifica la confirmación completa y cada aplicación usa una clave derivada de la operación y el animal, evitando duplicados durante reintentos de la PWA.

Spring valida elegibilidad, calcula la próxima aplicación, la fecha de alerta y los retiros de carne y leche proporcionados por `ProductoSanitarioPort`. Para vacunaciones recurrentes programa `VACUNACION_PROXIMA` con fecha programada y fecha de vencimiento. Supabase solo procesa posteriormente las alertas programadas.
