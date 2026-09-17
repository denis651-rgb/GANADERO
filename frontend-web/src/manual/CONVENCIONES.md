# Convenciones de redacción del manual

Este archivo no es un capítulo del manual — es la guía interna para escribir y
mantener los capítulos de `chapters/`. No se muestra dentro de Ganadero.

## Estructura de un capítulo

Todo capítulo que documente una funcionalidad (no aplica a la portada ni a las
preguntas frecuentes) sigue este orden de secciones:

```md
# Título de la funcionalidad

## Para qué sirve

Una o dos frases. Qué problema resuelve, no cómo funciona por dentro.

## Antes de comenzar

Lista de requisitos previos (qué debe existir ya en el sistema para poder
hacer esto). Si no hay requisitos, se omite la sección.

## Procedimiento

Pasos numerados, verbo en imperativo, nombres de pantalla/botón en **negrita**
tal como aparecen en la interfaz.

## Ejemplo

Un caso realista con datos concretos (no "Animal 1", "Proveedor X").

## Resultado esperado

Qué cambia en el sistema después de completar el procedimiento.

## Importante

Advertencias o matices no obvios, con `>` (blockquote). Opcional.

## Problemas frecuentes

Subtítulos `###` por cada problema, formato "síntoma" → causa/solución.
```

Un capítulo puede tener varias funcionalidades. Hay dos formas de organizarlas,
según qué tan parecidas sean entre sí:

**A. Variantes de una misma acción** (misma finalidad, mismo tipo de ejemplo y
de resultado — solo cambia el procedimiento). Se comparten `## Para qué
sirve`, `## Antes de comenzar`, `## Ejemplo`, `## Resultado esperado` e
`## Importante` a nivel de capítulo, y cada variante es su propia sección con
solo los pasos: `## Compra individual` / `## Compra por lote` (ver
`04-compras.md`), o `## Pesaje individual` / `## Pesaje por lote`, o
`## Venta individual` / `## Venta por lote`.

**B. Operaciones distintas de un mismo dominio** (cada una con su propia
finalidad, requisitos y ejemplo — no tiene sentido compartirlos). Cada
operación es un mini-capítulo completo con su propio `### Para qué sirve` /
`### Antes de comenzar` / `### Procedimiento` / `### Ejemplo` / `### Resultado
esperado`, bajo un `##` con el nombre de la operación. Así se organizan
`08-reproduccion.md` (celo, servicio, diagnóstico, parto, aborto, destete) y
`09-sanidad.md` (plan sanitario, jornada, tratamientos, calendario). Solo
`## Problemas frecuentes` va una vez, al final del capítulo, para todas las
operaciones.

Ante la duda: si dos variantes comparten la misma respuesta a "¿para qué
sirve esto?" y a "¿qué ejemplo pondrías?", son forma A. Si no, son forma B.

## Tono y redacción

- Se le habla al usuario final (ganadero/administrador de finca), no a quien
  programó el sistema. Sin jerga técnica (sin "endpoint", "estado ENUM",
  "movimiento tipo INGRESO_COMPRA" — eso es "cuando registras una compra").
- Los nombres de pantallas, menús y botones se escriben exactamente como
  aparecen en la interfaz, en **negrita**.
- Frases cortas. Un paso del procedimiento es una acción, no dos.
- Español neutro, tratamiento de "tú" (coherente con el resto de la app).

## Imágenes

- Se guardan en `frontend-web/public/manual-images/<capítulo>/`, por ejemplo
  `public/manual-images/compras/compra-lote.jpg` — **no** en `src/manual/images/`. Los
  capítulos son texto plano (`?raw`) sin procesar por el bundler, así que una ruta relativa
  tipo `../images/...` se resuelve contra la URL de la página (`/manual/compras`), no contra
  el archivo del capítulo, y no carga nada; además `src/` no se copia al build de producción.
  `public/` sí se sirve tal cual, tanto en dev como en el build empaquetado con Electron.
- En el Markdown, la ruta va absoluta desde la raíz del sitio, con el mismo nombre de carpeta:
  `![Formulario para registrar una compra individual](/manual-images/compras/compra-individual.jpg)`.
- Siempre con texto alternativo descriptivo.
- Recortadas para mostrar solo el área relevante, mismo tamaño/resolución
  dentro de un mismo capítulo.
- Nunca con datos reales de un cliente/proveedor/animal — usar los mismos
  datos de ejemplo que use el texto del capítulo.
- Se reemplazan cuando la interfaz cambia de forma significativa; no hace
  falta actualizarlas por cambios cosméticos menores.

## Enlaces internos entre capítulos

Los encabezados `##`/`###` generan anclas estables (slug del título en
minúsculas, con guiones). Para enlazar a una sección de otro capítulo:
`[registrar un parto](./08-reproduccion.md#registrar-parto)`. Si se renombra
un encabezado que ya tiene enlaces apuntándole desde otro capítulo, hay que
actualizar esos enlaces a mano — no hay verificación automática todavía.

## Mantenimiento

Una funcionalidad visible para el usuario no se considera terminada hasta que
su capítulo del manual haya sido actualizado (nuevo procedimiento, captura
nueva si cambió la pantalla, y `updatedAt` de `manual.json` al día).
