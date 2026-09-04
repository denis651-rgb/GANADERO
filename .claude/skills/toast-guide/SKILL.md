---
name: toast-guide
description: Guide for Ganadero's own toast system (frontend-web/src/shared/toast) — setup, when to use each tone, and troubleshooting. This project does NOT use Sonner; do not add that dependency.
---

# Toasts en Ganadero (skill propia, no Sonner)

Adaptado de la skill `ask-sonner` de emilkowalski/skills. Este proyecto **no usa
Sonner ni ninguna librería de toasts** — tiene su propia implementación pequeña en
`frontend-web/src/shared/toast/` (`useToast.ts` + `ToastProvider.tsx`). Esta skill
documenta esa implementación real, no la API de Sonner.

**No instales `sonner` ni `react-hot-toast`.** Si una tarea pide "un toast", es casi
seguro que se resuelve con lo que ya existe.

## Setup (ya hecho, no lo dupliques)

- `<ToastProvider>` está montado **una sola vez**, en
  `frontend-web/src/app/providers/AppProviders.tsx:31`, envolviendo toda la app.
  No montes un segundo `<ToastProvider>` en ninguna pantalla — duplicaría la región
  de toasts.
- Para mostrar un toast desde cualquier componente hijo:

```tsx
import { useToast } from '@/shared/toast/useToast'

const { showToast } = useToast()
showToast('Guardado correctamente')            // tono por defecto: success
showToast('No se pudo guardar', 'danger')
showToast('Sincronizando datos…', 'info')
```

`useToast()` solo funciona dentro del árbol de `<ToastProvider>`; si se llama afuera
lanza `Error('useToast debe usarse dentro de ToastProvider')`
(`useToast.ts:13`) — esa excepción es la señal de que el componente no está
montado donde debería.

## Elegir el tono correcto

La API real es `showToast(message: string, tone?: 'success' | 'danger' | 'info')`,
nada más. No existen (todavía) las variantes de Sonner:

| Querés | Tenés | No existe (habría que construirlo) |
| --- | --- | --- |
| Confirmar éxito | `showToast(msg)` o `showToast(msg, 'success')` | — |
| Error | `showToast(msg, 'danger')` | — |
| Info neutra | `showToast(msg, 'info')` | — |
| Loading → success/error encadenado | — | no hay `toast.loading`/`toast.promise` |
| Botón de acción dentro del toast | — | no hay `{ action }` |
| Duración por-llamada / `Infinity` | — | duración fija (ver abajo), no es parametrizable hoy |

Si una feature realmente necesita loading→success o un botón de acción, es trabajo
nuevo sobre `ToastProvider.tsx` (agregar campos a la interfaz `Toast`), no algo para
improvisar con un toast casero dentro del componente.

## Comportamiento real (para no asumir de más)

- Auto-dismiss fijo a **4500ms** (`ToastProvider.tsx:28`), igual para los tres
  tonos. No hay parámetro de duración por llamada.
- Botón de cierre manual ya incluido (`.toast-close`, `ToastProvider.tsx:39`) —
  no hace falta pedir uno.
- Los toasts se apilan en `.toast-region` (arriba-derecha, ancho máx. 420px,
  `index.css:294`). Mensajes largos van a wrappear en varias líneas: no hay un
  slot de `description` separado como en Sonner, así que conviene un mensaje
  corto de una sola idea.
- La animación de entrada (`toast-slide-in`, 350ms ease-out, `index.css:303`) ya
  respeta el bloque global `@media (prefers-reduced-motion: reduce)` al final de
  `index.css` (todas las animaciones bajan a 0.01ms) — no hace falta guardarla aparte.

## Troubleshooting

| Síntoma | Causa → arreglo |
| --- | --- |
| `useToast debe usarse dentro de ToastProvider` | El componente que llama a `useToast()` se renderiza fuera del árbol de `AppProviders`. Verificá que esté montado bajo `<ToastProvider>` (`AppProviders.tsx:31`). |
| El toast nunca aparece | `showToast` se llamó durante el render en vez de en un handler/efecto, o el componente no está realmente montado bajo el provider. |
| Toasts duplicados | Hay un segundo `<ToastProvider>` en algún lado — solo debe existir el de `AppProviders.tsx`. |
| El texto se ve apretado / corta mal | El mensaje es demasiado largo para 420px de ancho; acortalo en vez de pedir un `description` separado que no existe. |
| Quiero que no desaparezca solo | No hay `duration: Infinity` hoy; la única forma de "persistir" es que el usuario lo cierre con la ✕, o extender `ToastProvider` para soportar duración infinita. |
