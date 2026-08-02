# Despliegue en Render

Render usa `backend/Dockerfile`, Java 21 y build multietapa con `mvn clean verify`. Configure todas las variables de `backend/.env.example` como secretos del servicio. Mantenga bootstrap y estado técnico deshabilitados. El health check es `/actuator/health` y no expone detalles.
