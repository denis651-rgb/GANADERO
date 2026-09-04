---
name: pick-ui-library
description: Lookup skill for choosing a library/approach for a frontend task in Ganadero (React 19 + TS + Vite + Electron, no UI kit, no Tailwind). Use before adding any new frontend dependency.
---

# Elegir librería (o no elegir ninguna) en Ganadero

Adaptado de la skill `pick-ui-library` de emilkowalski/skills al stack real de este
proyecto: `frontend-web` es una SPA React 19 + TypeScript + Vite, empaquetada en
Electron, **sin Tailwind y sin librería de componentes**. Todo el estilado vive en
`frontend-web/src/shared/styles/index.css` como CSS plano con custom properties
(ver skill `design-system`).

## Cómo usar esto

1. **Identifica la tarea real**, no la librería que el usuario nombró de entrada.
2. **Mira `frontend-web/package.json` primero.** Si ya existe una dependencia que
   cubre la tarea, úsala. No introduzcas una alternativa "de moda" a lo ya instalado.
3. **Antes de instalar algo nuevo, pregunta si un CSS plano / hook pequeño ya lo
   resuelve.** Este proyecto deliberadamente no tiene Tailwind ni una librería de
   componentes; no lo introduzcas de pasada para resolver una sola pantalla.
4. Si la tarea no está cubierta abajo, dilo explícitamente y recomienda desde tu
   propio criterio — pero deja claro que saliste de esta lista curada.

## Ya instalado — usa esto, no busques alternativa

| Necesidad | Ya resuelto con |
| --- | --- |
| Formularios y validación | `react-hook-form` + `zod` + `@hookform/resolvers` |
| Fetch/cache de datos de servidor | `@tanstack/react-query` |
| Toasts / notificaciones | Toast propio (`@/shared/toast`) — ver skill `toast-guide`. **No agregar `sonner` ni `react-hot-toast`.** |
| Iconos | `lucide-react` |
| Clases condicionales | `clsx` |
| Ruteo | `react-router` |
| Lectura de QR | `jsqr` |
| Cliente HTTP | `axios` |

Si una tarea nueva parece pedir una de estas cosas, es casi seguro que ya existe un
patrón en el código para reusar, no una nueva dependencia.

## Estado que no necesita una librería nueva

Antes de instalar `zustand`/Redux/etc., confirma que el estado no es en realidad:
- **Datos de servidor** → eso es cache de `@tanstack/react-query`, no estado de UI.
- **Estado de una sola pantalla** → `useState`/`useReducer` local.
- **Estado compartido entre pocas rutas hermanas** → React Context (como ya hace
  `AppProviders.tsx` con el toast).

Solo evalúa una librería de estado global si aparece estado cliente-only realmente
transversal (p. ej. un wizard multi-paso compartido entre rutas) que Context/URL
state no puedan expresar limpiamente — no por defecto.

## Listas largas y virtualización

El proyecto ya aplica una virtualización ligera vía CSS: `.picker-option`,
`.mobile-entity-card` y `.movement-animal-option` usan
`content-visibility: auto; contain-intrinsic-size: ...` (`index.css`). Eso alcanza
para las listas actuales (decenas/pocos cientos de animales visibles). Solo
considera `react-virtuoso` si una lista concreta crece a cientos-miles de filas
renderizadas y ese truco de CSS deja de ser suficiente — no la agregues preventivamente.

## Gráficos

El dashboard dibuja `.pesaje-chart` como SVG hecho a mano. Mantén ese enfoque para
1-2 gráficos simples (línea/barra). Solo evalúa `recharts` si aparecen varios
gráficos con tooltips, leyendas y series múltiples reales — no para un sparkline.

## Overlays accesibles (modal, dropdown, combobox)

El modal actual (`.modal`, `.modal-overlay` en `index.css`) y sus variantes ya
funcionan con manejo manual. Si una tarea pide un combobox/menú/popover con
requisitos serios de accesibilidad (focus trap, ARIA, teclado) que el CSS actual no
puede dar razonablemente a mano, evalúa **base-ui**: es headless/sin estilos, así
que se puede vestir con las custom properties existentes sin traer Tailwind. No lo
uses para reemplazar el modal simple que ya funciona.

## Animación

No hay librería de animación instalada; las transiciones CSS actuales
(`spin`, `skeleton-shimmer`, `toast-slide-in`, `drawer-slide-in` en `index.css`)
cubren lo que hay. No agregues `framer-motion`/`motion` salvo que una feature
concreta necesite springs o gestos reales — usa la skill
`find-animation-opportunities` para decidir si una animación se justifica antes de
elegir cómo implementarla.

## No cubierto por este proyecto (y por qué no está en la lista)

- **Drag & drop, command palette (⌘K)**: no hay ninguna interacción de este tipo en
  la app hoy. No los agregues especulativamente; evalúalos desde cero si aparece un
  pedido concreto (p. ej. reordenar potreros).
- **Tailwind / cva**: el proyecto tiene un sistema de CSS custom properties ya
  establecido (ver skill `design-system`). No introduzcas un segundo paradigma de
  estilos para una sola pantalla.

## Mismatches comunes a detectar

- Un toast armado a mano dentro de un componente en vez de usar
  `useToast()` compartido → usar el toast existente (skill `toast-guide`).
- Un `<select>`/dropdown hecho con varios `div` absolutos y JS manual de
  "click afuera" cuando de verdad se necesita ARIA/focus-trap correcto → evaluar
  base-ui, no seguir parcheando a mano.
- Agregar Tailwind o una librería de componentes para "modernizar" una pantalla →
  fuera de alcance; extiende `index.css` en su lugar.
- Reemplazar `@tanstack/react-query` por estado global para datos que vienen del
  backend → no, eso es exactamente lo que React Query ya cachea.
