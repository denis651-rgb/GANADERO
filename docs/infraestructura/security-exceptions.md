# Excepciones de seguridad

## GHSA-qwww-vcr4-c8h2 — React Router RSC

Excepción temporal para `react-router-dom` 7.18.2. El advisory afecta el procesamiento de acciones en modo React Server Components. GANADERO se compila como SPA cliente con Vite y no usa RSC, loaders/actions de servidor ni Server Actions, por lo que el camino vulnerable no está presente.

La excepción debe revisarse cuando React Router publique una versión fuera del rango afectado (`>=7.12.0 <8.3.0`) o si el frontend adopta renderizado de servidor/RSC. Cualquier otro advisory alto o crítico continúa bloqueando CI.
