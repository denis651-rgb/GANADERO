---
name: find-animation-opportunities
description: Sweep the Ganadero UI for moments that would genuinely benefit from motion, and propose CSS-only recipes. High restraint by design — this is a dense, daily-use desktop tool for ranch staff, not a marketing site. Does not implement, only reports.
---

# Buscar oportunidades de animación en Ganadero

Adaptado de `find-animation-opportunities` de emilkowalski/skills. Esta app es una
herramienta de escritorio de uso diario para operadores de campo (tablas de
animales, dashboards, formularios) — el principio de "a veces la mejor animación es
ninguna" pesa **más**, no menos, que en un producto consumer típico.

No hay librería de animación instalada (`frontend-web/package.json` no tiene
`framer-motion`/`motion`). Toda sugerencia acá es **CSS puro** (transitions/keyframes),
nunca springs ni gestos con física — esta app tampoco tiene interacciones de
drag/swipe, así que esa sección del skill original no aplica y se omite.

## Postura

Rechazá la mayoría de los candidatos. Una lista corta de alta convicción es mejor
que una wishlist larga. Esta skill **reporta, no implementa**.

## El Gate (igual que el original, es agnóstico de stack)

Cada candidato debe pasar las 4 preguntas, en orden:

### 1. Frecuencia
| Frecuencia | Veredicto |
| --- | --- |
| 100+ veces/día (navegación core, atajos) | Rechazar. Nunca animar. |
| Decenas de veces/día (hover, toggles frecuentes) | Rechazar, o solo movimiento casi imperceptible |
| Ocasional (modales, drawers, toasts, settings) | Elegible |
| Raro / primera vez (onboarding, empty states, éxito) | Elegible — acá vive el presupuesto de "delight" |

### 2. Propósito (debe nombrarse explícitamente)
Feedback / Consistencia espacial / Indicar estado / Evitar un salto brusco /
Explicación / Delight (solo en frecuencia rara). "Se ve cool" no cuenta.

### 3. Velocidad (presupuestos)
| Elemento | Duración |
| --- | --- |
| Feedback de press | 100–160ms |
| Tooltips, popovers chicos | 125–200ms |
| Dropdowns, selects | 150–250ms |
| Modales, drawers | 200–500ms |

### 4. Función
Si el dato es algo que el usuario está tratando de **leer o accionar** (una tabla
de animales, una métrica del dashboard), no debería moverse por estética.

## Vocabulario existente — extendé esto, no inventes tokens nuevos

`frontend-web/src/shared/styles/index.css` ya tiene animaciones reales; cualquier
sugerencia nueva debe usar duraciones/easings cercanos a estos, no valores
arbitrarios:

| Animación | Duración/easing | Dónde |
| --- | --- | --- |
| `spin` | 0.8s linear | `index.css:197-198` |
| `skeleton-shimmer` | 1.4s ease | `index.css:201-202` |
| `toast-slide-in` | 0.35s ease-out | `index.css:303` |
| `drawer-slide-in` | 0.22s ease-out | `index.css:408` |

El bloque `@media (prefers-reduced-motion: reduce)` al final del archivo
(`index.css:~653`) ya baja **toda** animación/transición a 0.01ms globalmente —
no hace falta agregar el guard de reduced-motion por sugerencia como pide el skill
original; ya está resuelto a nivel global.

## Dónde buscar (adaptado — sin la sección de gestos/drag)

**Huecos de feedback**
- Elementos presionables sin `:active` → `transform: scale(0.97)` con
  `transition: transform 160ms ease-out`.

**Estado que "teletransporta"**
- Contenido que aparece/desaparece de golpe (renders condicionales, tabs,
  secciones que expanden) → fade/scale de entrada desde `scale(0.95-0.97)` +
  `opacity: 0`, `ease-out`.
- Acordeones/paneles que se abren de golpe → transición de `height` + `opacity`.

**Historia espacial faltante**
- Modales/popovers que aparecen sin conexión visual a su trigger → los modales
  centrados (`.modal-overlay`) quedan exentos, se mantienen centrados; para un
  popover/menú puntual, animar `transform-origin` desde el trigger.

**Entradas en grupo**
- Una grilla/lista que aparece toda junta en una pantalla poco visitada →
  stagger de 30-80ms, decorativo, nunca debe bloquear interacción.

**Presupuesto de delight**
- Momentos raros y de alta carga emocional (primer uso, empty state, éxito de
  una operación) — únicos lugares donde vale una animación más generosa.

Se omite deliberadamente la sección de "gesture seams" (drag/swipe con física de
resorte) del skill original: no hay ninguna interacción de arrastre en esta app.

## Formato de salida (igual que el original)

**Parte 1 — Tabla de oportunidades**, una fila por sugerencia sobreviviente,
ordenada por impacto, con ubicación exacta (`archivo:línea`), estado hoy,
propósito, frecuencia y la receta CSS exacta.

**Parte 2 — Candidatos rechazados (obligatorio)**: 2-5 lugares considerados y
descartados, con la pregunta del Gate que los mató. Esto es lo que distingue esta
skill de una wishlist.

**Parte 3 — Veredicto**: un párrafo corto sobre cuánta animación necesita
realmente esta interfaz (probablemente poca, dado el uso diario e intensivo en
datos) y cuál sugerencia tiene más impacto.
