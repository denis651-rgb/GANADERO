# Mi finca

Mi finca reúne lo que el sistema sabe de tu operación: los establecimientos
donde trabajas, los ajustes generales de la aplicación, cómo se clasifica el
hato por edad, los respaldos de tu información y la sincronización opcional
con Google Calendar.

Casi todo se configura una vez y se usa después de vez en cuando. Este
capítulo se organiza por pantallas, en el mismo orden que aparece en el menú.

## Propiedades

### Para qué sirve

Registrar los establecimientos de tu operación (puedes manejar varios) y, dentro
de cada uno, sus sectores. Las propiedades identifican dónde se encuentran tus
potreros y tus animales.

### Antes de comenzar

Tener a mano el nombre de cada establecimiento, su departamento, municipio y
superficie en hectáreas. No necesitas códigos: el sistema los asigna solo al
guardar (`PRP-001`, `PRP-001-SEC-001`...).

La aplicación crea automáticamente una propiedad llamada **Mi finca** al
instalarse. Lo primero es completarla con tus datos reales.

### Procedimiento

**Completar o crear una propiedad**

1. Abre **Mi finca** → **Propiedades**.
2. Para completar la propiedad existente, presiona el lápiz de editar y
   llena **Departamento**, **Municipio**, **Localidad**, **Referencia de
   dirección** y **Superficie (ha)** según corresponda.
3. Presiona **Guardar propiedad**.
4. Si la operación tiene más de un establecimiento, presiona **Nueva
   propiedad**, escribe el **Nombre** y presiona **Crear propiedad**.

**Añadir sectores**

1. En la lista de propiedades, presiona el ícono de mapa sobre la propiedad.
2. Presiona **Añadir sector**, escribe el **Nombre** y una **Descripción**
   opcional, y presiona **Añadir sector**.
3. Repite la operación por cada sector que quieras distinguir.

### Ejemplo

La estancia "El Roble" (Warnes, Santa Cruz, 1 200 ha) se completa editando
la propiedad **Mi finca**: se escribe el **nombre** real, ubicación y
superficie. Dentro de ella se crean los sectores **Norte**, **Laguna** y
**Gordo**.

### Resultado esperado

Cada propiedad aparece con su código, ubicación, superficie y el estado
**ACTIVA**. Al seleccionarla se ven sus sectores. Las propiedades y sectores
**ACTIVOS** quedan disponibles para registrar potreros y animales; los que
están **INACTIVOS** no aparecen en esas pantallas.

> Al desactivar una propiedad o sector, los potreros y animales existentes
> conservan su relación. Solo dejan de estar disponibles para datos nuevos.

## Configuración general

### Para qué sirve

Ajustar los valores con los que funciona toda la aplicación: unidades de
superficie, cuántos días antes se emiten las alertas de reproducción y
pesaje, cómo se guardan las fotografías y el nombre de usuario que se
muestra en la barra lateral. También incluye el apartado **Bloqueo con PIN**,
que todavía no está en funcionamiento (ver más abajo).

### Antes de comenzar

Decidir la unidad de superficie (hectáreas, metros cuadrados o acres) y
cuántos días antes de cada evento (parto, pesaje, destete, diagnóstico tras
el servicio) conviene recibir el aviso.

### Procedimiento

1. Abre **Mi finca** → **Configuración general**.
2. Ajusta la **Unidad de superficie** y los campos de días de alerta según
   necesites:
   - **Días de alerta antes del parto**: con cuánta anticipación aparece el
     aviso de **Parto próximo**, contando desde la fecha probable de parto
     calculada tras un diagnóstico de gestación positivo.
   - **Días sin pesaje para alertar**: cuántos días pueden pasar sin
     registrar un pesaje a un animal antes de que aparezca el aviso
     **Pesaje atrasado**.
   - **Días de alerta de destete**: con cuánta anticipación aparece el aviso
     **Destete próximo**, contando desde la fecha estimada de destete de
     cada cría.
   - **Días para diagnóstico post-servicio**: cuántos días después de
     registrar un servicio (monta o inseminación) el sistema recomienda
     hacer el diagnóstico de gestación y muestra el aviso correspondiente.
   - **Días de gestación estimada**: cuánto dura una gestación en tu
     operación. Se usa para calcular la fecha probable de parto cuando
     registras un diagnóstico positivo.
   - **Hora de los avisos**: a qué hora del día salen los avisos que nacen
     de una fecha sin hora (parto probable, destete, fin de un retiro y
     próxima vacunación declarada). Por defecto es **08:00**; los días de
     anticipación de arriba se cuentan desde esa fecha. Ver
     [A qué hora salen los avisos de una fecha](./10-alertas.md#a-que-hora-salen-los-avisos-de-una-fecha).
3. Ajusta cómo se guardan las fotografías:
   - **Calidad de imagen (1-100)**: qué tan comprimida queda cada foto
     nueva al subirla. Un número más bajo ocupa menos espacio pero se ve
     con menos nitidez; un número más alto conserva más detalle y pesa más.
   - **Comprimir fotos al subirlas**: si está marcada, cada foto que subas
     (por ejemplo en la ficha de un animal) se reduce de tamaño según la
     **Calidad de imagen**. Si la desmarcas, las fotos se guardan tal como
     las tomó la cámara, sin reducir su peso.
4. Escribe el **Nombre de usuario** que quieres ver en la aplicación.
5. Presiona **Guardar configuración**.

**Guardar un PIN (el bloqueo todavía no está en funcionamiento)**

1. En la misma pantalla, baja a **Bloqueo con PIN**.
2. Escribe el **Nuevo PIN** (entre 4 y 20 caracteres) y repítelo en
   **Confirmar PIN**. Si no coinciden, el sistema te avisa y no lo guarda.
3. Presiona **Configurar PIN**. Para cambiarlo, escribe uno nuevo y presiona
   **Cambiar PIN**. Para quitarlo, presiona **Quitar PIN**.

> **El bloqueo con PIN todavía no está en funcionamiento.** El PIN se guarda
> de forma cifrada, pero Ganadero no lo pide al abrirse, ni tampoco para
> cambiarlo o quitarlo. Por eso hoy **no protege el acceso**: cualquiera que
> abra la aplicación en este equipo ve los datos, haya o no un PIN guardado.
> La pantalla lo recuerda con un aviso amarillo en ese apartado.

### Ejemplo

La finca maneja superficie en hectáreas y quiere enterarse del parto 30 días
antes y del destete 7 días antes. Como suele fotografiar con el celular y
las fotos pesan mucho, deja marcada **Comprimir fotos al subirlas** con
**Calidad de imagen** en 75.

> Mientras el bloqueo con PIN no esté en funcionamiento, cualquiera que
> encienda la computadora puede abrir la aplicación y ver los datos. Para
> protegerla, usa la clave de inicio de sesión de Windows del equipo y no
> dejes la sesión abierta.

### Resultado esperado

Los cambios aplican de inmediato: las alertas futuras usan los nuevos días
de anticipación, las fotos que subas después se comprimen (o no) según lo
configurado, y el nombre visible se actualiza. Si guardaste un PIN, queda
almacenado, pero por ahora la aplicación no lo pide al abrirse.

> La **Zona horaria**, **Moneda** y **Unidad de peso** se muestran fijas,
> con los valores configurados en la instalación, y no son editables desde
> la interfaz.

> El plazo de aviso de cada vacunación o tratamiento sanitario no se
> configura aquí: se define por actividad al armar el plan sanitario (ver
> [Plan sanitario](./09-sanidad.md#plan-sanitario)), porque cada una puede
> necesitar un plazo distinto.

## Categorías por edad

### Para qué sirve

Definir los rangos de meses que determinan la categoría de cada animal según
su sexo. Son la base para clasificar automáticamente el hato y para los
requisitos de edad de las actividades sanitarias.

### Antes de comenzar

Conocer las categorías que quieres usar. La aplicación trae las más comunes
(Ternero/Ternera, Vaquilla/Novillo, Vaca/Toro). Solo es necesario cambiarlas
si tu clasificación es distinta.

### Procedimiento

1. Abre **Mi finca** → **Categorías por edad**.
2. Revisa las categorías por sexo: nombre, rango en meses, tipo
   (**Automática** o **Manual**) y estado.
3. Para crear una, presiona **Nueva categoría**, define **Nombre**, **Sexo
   aplicable** y las edades **mínima** y **máxima** (vacío = sin límite).
4. Desmarca "Calcular automáticamente por edad" para excepciones manuales
   como Buey.
5. La aplicación no te deja guardar rangos que se crucen con otra categoría
   activa, así que normalmente no necesitas tocar nada más. Solo si en algún
   momento dos categorías llegaran a aplicar a la misma edad (por ejemplo,
   tras desactivar y reactivar una), abre **Opciones avanzadas** y escribe
   un **Orden de evaluación** (0 o más; gana el número más bajo) para decidir
   cuál tiene prioridad.
6. Presiona **Simular impacto** para ver cuántos animales cambiarían antes
   de guardar, y después **Crear categoría** (o **Guardar cambios** al
   editar una existente).
7. Cuando cambien los rangos, presiona **Aplicar reclasificación ahora** para
   recalcular la categoría del hato con las nuevas reglas.

### Ejemplo

La finca quiere separar la **Vaquillona** (hembras de 13 a 22 meses) antes
de pasar a la **Vaquilla** (23 a 35 meses). Se crea la categoría nueva, se
simula el impacto (12 animales cambiarían) y se aplica la reclasificación:
cada animal pasa a su categoría nueva y queda el cambio anotado en su
historial.

### Resultado esperado

En la pantalla quedan las categorías activas con sus rangos. Tras la
reclasificación, el **Panel principal** muestra la nueva distribución por
categoría, y cada animal actualizado registra el cambio en su historial.

> Al asignar rangos que dejan un hueco sin cubrir (por ejemplo, del 13 al 20
> y del 25 al 40), la aplicación lo avisa y te pide confirmar que es
> intencional antes de guardar. Las excepciones manuales nunca se modifican
> en la reclasificación.

## Respaldos

### Para qué sirve

Proteger toda la información de la finca, guardándola de forma automática en
tu computadora y, si quieres, copiándola a una carpeta de Google Drive. Un
respaldo permite recuperar los datos si algo sale mal.

### Antes de comenzar

En el navegador solo se consulta; crea y restaura respaldos desde la
aplicación **Ganadero Desktop**. Si usarás Drive, define una carpeta
sincronizada destinada a los respaldos.

### Procedimiento

1. Abre **Mi finca** → **Respaldos**.
2. En **Configuración**, activa **Respaldo automático**, elige la
   **Frecuencia** (**Diaria**, **Semanal** o **Mensual**) y la **Hora**.
3. Define las **Retenciones** (cuántos respaldos diarios, semanales y
   mensuales se conservan) y, si quieres copia en Drive, presiona **Elegir
   carpeta…** en "Carpeta sincronizada (Google Drive)".
4. Presiona **Guardar configuración**.
5. Para un respaldo inmediato, presiona **Crear respaldo ahora**.
6. De vez en cuando, comprueba la lista: cada respaldo indica su integridad
   (**Íntegro**, **Sin verificar** o **Corrupto**). Puedes presionar
   **Verificar respaldos** o el ícono del ojo en cada fila.

**Restaurar un respaldo**

1. Presiona **Restaurar respaldo…** y elige el archivo.
2. Confirma la fecha y la versión que se muestran.
3. Escribe `RESTAURAR` en el campo y presiona **Restaurar**.

### Ejemplo

La finca configura un respaldo **Diario** a las 22:00, con retención de 7
diarios, 4 semanales y 12 mensuales, copiando a `G:\Mi unidad\Ganadero\
Respaldos`. Cada mañana, el último respaldo aparece **Íntegro** en el
historial.

### Resultado esperado

En **Resumen** se ven el último respaldo, la hora del próximo, si la copia
externa está configurada y si hubo errores. El **Historial** conserva los
respaldos según la retención configurada.

> Ganadero confirma la copia a la carpeta sincronizada, pero no puede ver si
> Google Drive terminó de subir: revísalo con el ícono de Drive. **Restaurar**
> reemplaza toda la información actual por la del respaldo; antes de hacerlo
> se crea un respaldo preventivo de lo que hay ahora.

## Google Calendar

### Para qué sirve

Enviar las actividades sanitarias (por edad, periódicas o con fecha
programada) a un calendario de Google, para llevar los compromisos de la finca
junto con el resto de tus eventos. Solo está disponible en Ganadero Desktop.

### Antes de comenzar

Contar con una cuenta de Google y tenerla abierta en el navegador para
autorizar la conexión cuando la aplicación lo pida.

### Procedimiento

1. Abre **Mi finca** → **Google Calendar**.
2. Presiona **Conectar Google** y autoriza el acceso con tu cuenta.
3. En **Nombre del calendario** escribe el que creará Ganadero (por ejemplo
   "Vacunación y sanidad") y presiona **Guardar configuración**.
4. Marca **Sincronizar automáticamente las actividades sanitarias** si
   quieres que se envíen solas.
5. Para enviar ahora los pendientes, presiona **Sincronizar ahora**.
6. Para dejar de usar la conexión, presiona **Revocar acceso**.

### Ejemplo

Con el calendario "Sanidad 2026" conectado, cada actividad **Periódica** del
plan sanitario genera el evento correspondiente con su fecha y horario, y los
pendientes se envían solos. En la pantalla se ve la cuenta autorizada y el
número de eventos en cola y de errores definitivos.

### Resultado esperado

La pantalla muestra el estado **AUTORIZADO**, la cuenta conectada y el
conteo de eventos. Los eventos aparecen en el calendario de Google; los que
fallan pueden reenviarse con **Reintentar errores**.

> Las actividades con modalidad **Manual** no generan eventos. Para que una
> actividad llegue al calendario debe ser **Por edad**, **Periódica** o de
> **Fecha programada**.

## Problemas frecuentes

### Configuré un PIN y la aplicación no me lo pide

Es lo esperado por ahora: el bloqueo con PIN todavía no está en funcionamiento.
El PIN se guarda, pero Ganadero no lo pide al abrirse, así que no protege el
acceso. Hasta que esté disponible, protege el equipo con la clave de inicio de
sesión de Windows. Si guardaste un PIN, puedes cambiarlo o quitarlo desde
**Mi finca** → **Configuración general** → **Bloqueo con PIN**, sin que te pida
el anterior.

### Al guardar me avisa que la propiedad o el sector cambió

Otra ventana o dispositivo modificó el dato mientras lo editabas. Presiona
**Recargar datos** para recuperar la versión actual y vuelve a aplicar tu
cambio.

### La reclasificación no cambia la categoría de un animal

Solo se reclasifican animales **activos** con fecha de nacimiento conocida o
estimada. Las excepciones manuales (como Buey) no se tocan, y el animal se
conserva en su categoría si el nuevo rango sigue dándole la misma.

### La aplicación pide confirmar un "hueco" entre categorías

Significa que los rangos no cubren todas las edades (por ejemplo, nada entre
los 13 y los 24 meses). Si el hueco es real (animales que se venden o se
clasifican a mano en esa edad), presiona **Confirmar hueco intencional**; si
fue un error, **Revisar rango** y corrige los meses.

### El respaldo no aparece en Google Drive

Revisa que la carpeta externa esté configurada y que la copia aparezca como
**Copiado a la carpeta externa** en el historial, y luego verifica en Drive
que la subida terminó (Ganadero no puede confirmarla por su cuenta). Un
respaldo en estado **Corrupto** no debe usarse para restaurar.

### No encuentro cómo conectar Google Calendar

Las funciones de conectar y revocar solo están disponibles en la aplicación
**Ganadero Desktop** (no en el navegador). Abre la pantalla desde ahí.

### "Conectar Google" se queda cargando y no puedo hacer nada más

Pasa cuando Google no completó la autorización en el navegador (por ejemplo,
un cliente OAuth mal configurado que Google rechaza antes de volver a
Ganadero). Presiona **Cancelar intento de conexión**, que aparece junto a los
botones mientras la conexión está en curso, y vuelve a intentarlo (importando
un JSON OAuth distinto si el problema era ese).