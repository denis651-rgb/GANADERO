# Seeds manuales

Estos scripts cargan datos de prueba y no son ejecutados por Flyway. Revise siempre
el nombre de la propiedad y del potrero antes de aplicarlos.

## 20 toros de engorde en Cerro Verde Uno

El archivo `seed_20_toros_cerro_verde_uno_corral.sql` agrega 20 machos Nelore,
categoría Toro, propósito Carne y origen Comprado. Busca por nombre la propiedad
`CERRO VERDE UNO` y el potrero `CORRAL`; si falta alguno o hay nombres repetidos,
cancela toda la transacción.

Es idempotente: puede ejecutarse nuevamente sin duplicar los 20 animales. Los
códigos `ANI-######` se obtienen de `core.secuencias_codigo`, igual que en la
aplicación.

Con PostgreSQL instalado localmente:

```powershell
psql -v ON_ERROR_STOP=1 -U ganadero -d ganadero -h localhost -p 55432 `
  -f database/seeds/seed_20_toros_cerro_verde_uno_corral.sql
```

Con la base iniciada mediante `infrastructure/compose.yaml`:

```powershell
Get-Content -Raw database/seeds/seed_20_toros_cerro_verde_uno_corral.sql |
  docker exec -i ganadero-postgres psql -v ON_ERROR_STOP=1 -U ganadero -d ganadero
```
