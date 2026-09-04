---
name: design-system
description: Ganadero's design tokens (green palette), typography, and accessibility checklist — scoped to the existing frontend-web/src/shared/styles/index.css. Use before adding new colors, type sizes, or UI states.
---

# Sistema de diseño de Ganadero

Extraído y adaptado de la parte de "design system" de `ui-ux-pro-max-skill`
(tokens/paleta/tipografía/accesibilidad), **descartando deliberadamente** el
catálogo de 79 estilos de tendencia (glassmorphism, bento grid, brutalism, etc.),
las 192 paletas por industria, los 74 pairings de Google Fonts y las plantillas
Tailwind/shadcn de ese repo — nada de eso encaja con una app utilitaria de un solo
color de marca, sin Tailwind, para gestión ganadera.

**Fuente única de verdad:** `frontend-web/src/shared/styles/index.css`. Es CSS
plano con custom properties, sin Tailwind ni CSS-in-JS. Toda UI nueva reutiliza
estos tokens; no se introduce un segundo sistema de estilos.

## Tokens ya definidos (`:root`, `index.css:1-21`)

```css
--green-900: #123d2a;   --green-50:  #edf7f1;
--green-800: #195436;   --primary:   #287b52;  /* = green-600 */
--green-700: #1f6a45;   --ink:       #1c2b22;
--green-600: #287b52;   --muted:     #66746b;
--green-100: #dceee3;   --border:    #dce5de;
                         --surface:   #ffffff;
--danger:  #b33a3a;
--warning: #a46414;
--shadow:  0 12px 30px rgba(24, 58, 38, 0.08);
```

Reglas de uso:
- Texto/UI primaria → `--primary` / `--green-700` / `--green-800`.
- Fondos suaves de marca → `--green-50` / `--green-100`.
- Nunca escribas un hex nuevo si un token existente ya lo cubre.

## Gap real: colores semánticos sin tokenizar

`--danger` y `--warning` existen, pero **no hay `--info` ni `--success` explícitos**,
y encima varias reglas repiten literales en vez de usar los tokens que sí existen:

- Info se repite como `#28516b` / `#eaf5fb` / `#c8e4f2` en al menos
  `.alerta-info`, `.alert-info`, `.status-syncing`, `.age-range-feedback`
  (varias líneas de `index.css`).
- Success reusa `--green-800`/`--green-50` de forma consistente en
  `.alert-success` — ese caso está bien.
- Danger/warning tienen token en `:root` pero varias reglas igual repiten sus
  hex equivalentes (`#862a2a`, `#fff0f0`, `#8c5716`, `#fff1dc`) en vez de
  referenciarlos.

**Cuando toques una de esas zonas:** agregá `--info` / `--info-bg`,
`--danger-bg`, `--warning-bg` a `:root` y hacé que la regla los use, en vez de
escribir un hex nuevo. No hace falta una migración masiva de todo el archivo de
una sola vez — conviene consolidar cada bloque la próxima vez que se edite.

## Tipografía

`index.css:2` declara `font-family: Inter, ui-sans-serif, system-ui, ...`, pero
**Inter nunca se carga** (no hay `<link>` a Google Fonts ni `@font-face` en
`index.html` ni en `main.tsx`). En la práctica todo usuario ve el fallback
(`system-ui`/`-apple-system`/`Segoe UI`), no Inter. Antes de agregar un font nuevo,
resolvé esto primero, con una de dos:
- Cargar Inter de verdad (self-host del `.woff2` o `<link>` de Google Fonts en
  `index.html`), si la marca realmente quiere Inter; o
- Sacar "Inter" del stack y declarar honestamente la pila de fuentes del sistema
  — encaja con una sensación de app de escritorio y no cuesta nada.

No hay una escala tipográfica centralizada (`--text-*`); cada heading usa su propio
`clamp()` (p. ej. `.page-header h1`, `.auth-hero h1`). Es razonable para las 2-3
jerarquías que hay hoy. Solo vale la pena introducir tokens `--text-*` si aparece
una tercera/cuarta jerarquía y los tamaños empiezan a divergir sin criterio — no
antes.

## Checklist de accesibilidad (antes de shipear un color o tamaño nuevo)

**Contraste (WCAG 2.1):**

| Nivel | Texto normal | Texto grande | Componentes UI |
| --- | --- | --- | --- |
| AA | 4.5:1 | 3:1 | 3:1 |
| AAA | 7:1 | 4.5:1 | 4.5:1 |

Fórmula de referencia (luminancia relativa → ratio de contraste):

```js
function luminance(r, g, b) {
  const [rs, gs, bs] = [r, g, b].map(v => {
    v /= 255
    return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4)
  })
  return 0.2126 * rs + 0.7152 * gs + 0.0722 * bs
}
function contrastRatio(l1, l2) {
  const [lighter, darker] = l1 > l2 ? [l1, l2] : [l2, l1]
  return (lighter + 0.05) / (darker + 0.05)
}
```

Ejemplo con la paleta real (verificar con una herramienta antes de confiar de
memoria): `--primary` (#287b52) como texto sobre `--surface` blanco ronda ~4.6:1,
al límite de AA para texto normal. `.button-primary` en cambio pone texto blanco
sobre `--green-700`/`--green-800` de fondo — es la dirección opuesta y hay que
chequearla por separado al reusar un token en un contexto nuevo.

**Tamaños mínimos:** el body usa 16px base. El punto más chico que ya existe en la
app es `.66rem` (~10.5px) en badges como `.module-status`/`.eyebrow` — tratalo
como piso, no bajes de ahí para texto nuevo.

**Motion y foco:** `index.css` ya define `@media (prefers-reduced-motion: reduce)`
(línea ~653) y `:focus-visible` global (línea 35). Toda UI interactiva nueva debe
heredar esto — no agregues animación o estilos de foco custom que lo esquiven.

## Explícitamente fuera de esta skill

No se portó del repo original (y no debería agregarse sin una razón concreta):
catálogo de 79 "styles" de tendencia, las 192 paletas por industria, los 74
pairings de Google Fonts, el recomendador de tipos de gráfico, y los scripts de
generación de tokens Tailwind/shadcn — nada de eso aplica a una app de una sola
marca, sin Tailwind, para operadores de campo.
