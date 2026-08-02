# Primeros pasos — Fase 0

1. Abrir `ganadero-backend` en IntelliJ IDEA.
2. Configurar JDK 21.
3. Esperar que Maven descargue las dependencias.
4. En una terminal, ejecutar `docker compose up -d`.
5. Ejecutar `GanaderoApplication` con el perfil `local`.
6. Abrir:
   - `http://localhost:8080/actuator/health`
   - `http://localhost:8080/api/v1/system/status`
7. Ejecutar la prueba `ModularityTest`.
8. Conectar pgAdmin:
   - Host: `localhost`
   - Puerto: `55432`
   - Base: `ganadero`
   - Usuario: `ganadero`
   - Contraseña: `ganadero_local_change_me`

Al iniciar, Flyway crea las extensiones `pgcrypto`, `postgis` y todos los esquemas definidos en la guía maestra.
