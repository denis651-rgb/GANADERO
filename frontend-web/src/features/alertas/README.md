# Módulo de alertas

El centro de alertas y las notificaciones Push son canales independientes:

- El centro muestra todas las alertas que el usuario tiene autorización para consultar.
- Las preferencias personales solamente deciden qué categorías y prioridades generan Push.
- Desactivar una preferencia no elimina, resuelve ni oculta la alerta.

Las categorías funcionales son reproducción, sanidad, tratamiento, pesaje,
movimiento, inventario y sistema. Cada `TipoAlerta` del backend pertenece a
exactamente una categoría.
