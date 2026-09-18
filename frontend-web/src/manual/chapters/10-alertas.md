# Alertas

El módulo **Alertas** es el centro de avisos de GANADERO: reúne los eventos
que requieren seguimiento (tratamientos próximos o atrasados, casos clínicos
críticos, partos próximos, diagnósticos pendientes, pesajes atrasados,
cuarentenas por finalizar, movimientos pendientes, entre otros) y además te
permite **programar recordatorios sanitarios** propios con fecha, hora y
repeticiones.

La mayoría de las alertas se generan solas a partir de lo que registras en
Sanidad, Reproducción, Pesajes o Movimientos: no hace falta crearlas. Tu
trabajo es revisarlas, atenderlas y resolverlas.

## Revisar el centro de alertas

### Para qué sirve

Ver de un vistazo qué necesita atención hoy. El centro muestra contadores por
filtro (**Pendientes**, **Urgentes**, **Hoy** y **Resueltas**), permite buscar
por título o mensaje y abre cada alerta en el módulo donde corresponde
resolverla.

### Antes de comenzar

Tener información registrada en los módulos (sanidad, reproducción, pesajes,
movimientos). Cada evento programado o pendiente produce una alerta con su
**prioridad** (**INFO**, **WARNING**, **URGENTE** o **CRITICA**).

### Procedimiento

1. Abre **Alertas** en el menú lateral.
2. Revisa los contadores superiores y elige un filtro:
   - **Pendientes**: todas las que todavía no se resolvieron.
   - **Urgentes**: solo las de prioridad **URGENTE** o **CRITICA**.
   - **Hoy**: las programadas para la fecha actual.
   - **Resueltas**: las que ya se cerraron.
3. Usa el buscador "Buscar por título o mensaje…" para filtrar.
4. Presiona **Ver** en una alerta para abrirla en el módulo que la origina
   (por ejemplo, un tratamiento en **Sanidad** o la ficha del animal en
   **Animales**).

### Ejemplo

El lunes aparecen en **Alertas**: 3 **Pendientes**, de las cuales 1 es
**URGENTE** ("Tratamiento atrasado" de un caso clínico), y 1 programada para
hoy ("Próximos diagnósticos recomendados" de una vaca). Se abre la urgente con
**Ver** para pasar a Sanidad.

### Resultado esperado

Ves cada alerta con su **prioridad**, **título**, **mensaje**, fecha
programada y **estado**. Los contadores y las listas se actualizan solos
(mientras la app está abierta se refrescan periódicamente).

> Las alertas no se crean a mano: surgen de lo que registras. Si falta un
> aviso, revisa que el dato esté registrado y con la fecha correcta en su
> módulo.

### Atención requerida en el Panel principal

El **Panel principal** resume en **Atención requerida** lo que necesita tu
atención hoy, ordenado de lo más urgente a lo más informativo. Cada fila
tiene un color (rojo si es urgente, ámbar si es una advertencia, azul si es
informativa) y, cuando corresponde, un botón que lleva a la pantalla donde se
resuelve.

| Aviso | Botón |
| --- | --- |
| **Animales sin pesaje reciente**: animales activos que nunca se pesaron o cuyo último pesaje es de hace más de 30 días. | **Registrar pesaje** |
| **Sanidad**: vacunas y actividades sanitarias próximas o vencidas, revisiones sanitarias de ingreso pendientes, retiros de carne o leche vigentes, cuarentenas por finalizar, casos clínicos críticos y recordatorios de sanidad. | **Ir a Sanidad** |
| **Tratamientos** próximos o atrasados. | **Ir a Sanidad** |
| **Reproducción**: celos detectados, diagnósticos de gestación pendientes, partos próximos y destetes próximos. | **Ir a Reproducción** |
| **Movimientos pendientes**. | **Ir a Movimientos** |
| **Inventario bajo** y avisos del sistema. | **Ver alertas** |
| **Potreros inactivos** (solo informativo). | — |

Cada fila agrupa las alertas del mismo tipo y dice cuántas hay, por ejemplo
"3 registros requieren atención". El color es el de la alerta más urgente del
grupo: las alertas críticas y urgentes van en rojo. El contador **N
pendientes** cuenta filas, no animales.

> Solo aparecen las alertas pendientes cuya fecha ya llegó. Las programadas
> para más adelante y las que ya atendiste o resolviste no se muestran. El
> pesaje se resume en una única fila con su botón: las alertas de pesaje
> atrasado del centro de alertas no se repiten en el Panel principal.

### A qué hora salen los avisos de una fecha

Algunos avisos nacen de una **fecha sin hora**: el parto probable, el destete,
el fin de un retiro de carne o de leche y la próxima vacunación que declaraste
en el historial de un animal. Esos avisos salen a la **Hora de los avisos**
(por defecto **08:00**, hora de Bolivia) del día que corresponde, y no a la
medianoche, cuando nadie los vería.

- Los **días de anticipación** siguen siendo los de **Configuración general**.
  Por ejemplo, con **Días de alerta antes del parto** en *15* y un parto
  probable el 30/08, el aviso sale el 15/08 a las 08:00.
- Si el día del aviso ya pasó cuando registras el dato (por ejemplo, un parto
  probable a solo 3 días con 15 días de anticipación), el aviso aparece
  enseguida, sin esperar a la hora de avisos.
- Para cambiarla, ve a **Mi finca** → **Configuración general**, edita **Hora
  de los avisos** y presiona **Guardar configuración**. El cambio vale para los
  avisos que se programen desde ese momento; los que ya estaban programados
  conservan su hora.
- Los recordatorios que programas tú, los pesajes atrasados y las actividades
  del calendario sanitario usan su propia hora y no cambian.

## Atender y resolver alertas

### Para qué sirve

Gestionar cada alerta hasta cerrarla. **Atender** indica que ya la estás
tratando; **Resolver** la cierra para que deje de contar como pendiente.

### Antes de comenzar

Tener la alerta activa (cualquier estado que no sea **RESUELTA** ni
**CANCELADA**). Es recomendable haberla abierto con **Ver** para saber qué
hacer.

### Procedimiento

1. En **Alertas**, ubica la alerta activa.
2. Presiona **Atender** para marcarla como atendida (deja de ofrecerse este
   botón para esa alerta).
3. Cuando el trabajo esté hecho, presiona **Resolver** para cerrarla.
4. Verifica que desapareció del filtro **Pendientes** y que el contador
   bajó.

### Ejemplo

La alerta **URGENTE** "Tratamiento atrasado" se atiende pasando a Sanidad a
aplicar el tratamiento; al registrarlo, se vuelve a **Alertas** y se presiona
**Resolver** sobre esa misma alerta.

### Resultado esperado

La alerta queda con estado **RESUELTA** (o **CANCELADA** si el sistema la
descarta por sí mismo al resolverse el origen) y deja de contar en
**Pendientes** y **Urgentes**. El historial se conserva en el filtro
**Resueltas**.

> Resolver la alerta no resuelve el problema de fondo: el seguimiento
> sanitario, reproductivo o del movimiento se actualiza en su propio módulo.

## Programar recordatorios

### Para qué sirve

Agendar avisos sanitarios propios (por ejemplo "vacunar lote de terneras") con
fecha y hora del evento, primera notificación, cantidad de avisos e intervalo
entre ellos. A diferencia de las alertas automáticas, estos avisos los creas
tú.

### Antes de comenzar

- Tener permiso para configurar alertas (el botón **Programar recordatorio**
  solo aparece si tu usuario puede hacerlo).
- Definir el **Título**, la **Prioridad** y el **Mensaje** del aviso, la
  **Fecha y hora del evento**, cuándo inicia la **Primera notificación** y
  cuántos **avisos** quieres.

### Procedimiento

1. En **Alertas**, en la sección **Recordatorios programados**, presiona
   **Programar recordatorio**.
2. En la ventana **Programar recordatorio sanitario**, escribe el **Título**
   y el **Mensaje**, y elige la **Prioridad** (**Informativa**, **Importante**,
   **Urgente** o **Crítica**).
3. Completa la **Fecha y hora del evento** y la **Primera notificación**.
   Escribe la **Cantidad de avisos** (máximo 10).
4. Si son más de un aviso, elige el **Intervalo entre avisos** (**15
   minutos**, **30 minutos**, **1 hora**, **2 horas**, **6 horas** o **1
   día**).
5. Revisa la **Vista previa** con la lista de avisos. Todos deben ocurrir
   antes del evento; si uno queda después, el sistema lo avisa y no deja
   guardar.
6. Presiona **Guardar recordatorio**.

**Pausar, reanudar o cancelar un recordatorio**

1. En **Recordatorios programados**, ubica el recordatorio **ACTIVO** o
   **PAUSADO**.
2. Presiona **Pausar** para detener los avisos restantes, **Reanudar** para
   volver a activarlo o **Cancelar** para darlo de baja.

### Ejemplo

Se programa "Vacunar lote de terneras": evento el 20/09/2026 a las 08:00,
primera notificación el 19/09 a las 17:00, 2 avisos con intervalo de **1
hora**. La vista previa muestra "Aviso 1" y "Aviso 2", ambos antes del evento.

### Resultado esperado

El recordatorio aparece en **Recordatorios programados** con su estado
**ACTIVO**, título, mensaje y "Evento: {fecha} · {n}/{total} avisos
generados". Se generan las notificaciones en las fechas programadas; al
completarlas pasa a **COMPLETADO**.

> Los avisos de un recordatorio no pueden ocurrir después del evento: verifica
> antes la **Vista previa**. El botón **Programar recordatorio** requiere
> permiso de configuración de alertas.

## Problemas frecuentes

### No veo el botón "Programar recordatorio"

La opción de programar recordatorios solo aparece con permiso de
configuración de alertas. Si tu usuario no lo tiene, pídelo a quien
administra los permisos.

### La alerta que espero no aparece

Las alertas se generan a partir de los registros con sus fechas. Revisa en el
módulo correspondiente (Sanidad, Reproducción, Pesajes, Movimientos) que el
evento exista y que la fecha esté bien; por ejemplo, un diagnóstico pendiente
sale de un servicio reciente. También verifica el filtro: las resueltas
quedan en **Resueltas**.

### Resolví la alerta pero el problema sigue

Resolver cierra la alerta en este módulo; el cambio real se hace en su módulo
de origen (aplicar el tratamiento, confirmar el movimiento, registrar el
pesaje). Haz el trabajo primero y resuelve la alerta después.

### Pusieron de más de 10 avisos

La **Cantidad de avisos** tiene un máximo de 10. Reduce la cantidad o programa
varios recordatorios con horarios distintos.

### El dato guardado no genera la notificación

La **Primera notificación** y todos los avisos deben ser anteriores al evento
y no pueden estar en el pasado. Corrige las fechas en la **Vista previa**
antes de **Guardar recordatorio**.