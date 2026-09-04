# Control de gestaciones

La migración V11 añade `gestacion_ciclo` y el vínculo de sus diagnósticos, partos y abortos. No reconstruye ni corrige automáticamente registros históricos.

## Operación

- Un nuevo diagnóstico positivo abre una gestación. Los diagnósticos sucesivos compatibles se asocian al mismo ciclo.
- Solo hay una gestación abierta por hembra. Un servicio no puede reutilizarse en otro ciclo.
- En Registrar parto / Registrar aborto debe seleccionarse una gestación abierta.
- El cierre pasa a `FINALIZADA_PARTO` o `FINALIZADA_ABORTO`. No admite otro desenlace ni reapertura directa.
- El cierre y el registro del evento/crías participan en la misma transacción: si falla la creación, se revierte todo.
- El diagnóstico positivo se conserva como hecho histórico; el estado final pertenece a la gestación.
- Las alertas de los diagnósticos asociados se resuelven al cerrar el ciclo.
- Un diagnóstico dudoso posterior no elimina la gestación ni devuelve su servicio a pendiente de diagnóstico.

## Antecedentes desconocidos e históricos

En los formularios de parto/aborto, cuando no hay gestación abierta, se puede identificar un diagnóstico positivo previo o guardar una gestación con antecedentes desconocidos. Esta última exige fecha de confirmación y una explicación de los antecedentes/evidencia; el inicio estimado es opcional. No genera un servicio ni un padre ficticio. Se guarda al pulsar Guardar gestación, incluso si luego se cancela el desenlace.

Los partos y abortos anteriores a V11 quedan sin vínculo. Sus fechas sí se consideran para impedir abrir una nueva gestación que se solape con el último desenlace registrado. Si hay registros históricos erróneos, requieren revisión; no se eliminan crías ni se reasignan eventos automáticamente.

## Fechas

- Servicio no anterior al celo asociado y posterior al último desenlace.
- Diagnóstico posterior al servicio y al último desenlace de un ciclo anterior.
- Inicio de nueva gestación posterior al último desenlace y al nacimiento de la madre.
- Confirmación no futura ni anterior al inicio estimado.
- Desenlace no futuro, posterior al inicio estimado y no anterior a la confirmación ni al último diagnóstico asociado.
- Comparaciones de días en America/La_Paz; parto y aborto tienen precisión de fecha, no hora.

No se impone una duración biológica mínima fija ni se infiere una fecha de concepción cuando es desconocida.

## Verificación

`GestacionServiceTest`: SQLite temporal con claves foráneas, cierre parto/aborto mutuamente excluyente, nuevo ciclo posterior, fechas, duplicados, hembra incorrecta, cierre condicional, rollback, diagnósticos sucesivos y antecedentes sin servicio ficticio.

`GestacionSelect.test.tsx`: solo abiertas seleccionables, historial visible y registro explícito sin antecedentes.

Reiniciar el backend para aplicar V11. No ejecutar ediciones manuales de estado sobre la base de datos.
