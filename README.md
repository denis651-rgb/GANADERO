# GANADERO Backend — Fase 0

Proyecto base de Spring Boot para construir GANADERO como **monolito modular**.

## Tecnologías

- Java 21
- Spring Boot 4.1.0
- Spring Modulith 2.1.0
- PostgreSQL + PostGIS
- Flyway
- Spring Security Resource Server
- Actuator
- Docker Compose

## Ejecutar localmente

### 1. Levantar PostgreSQL/PostGIS

```powershell
docker compose up -d
```

### 2. Abrir en IntelliJ

- Abrir la carpeta como proyecto Maven.
- Seleccionar JDK 21.
- Ejecutar `bo.com.ganadero.GanaderoApplication`.
- El perfil predeterminado es `local`.

### 3. Comprobar

```text
GET http://localhost:8080/actuator/health
GET http://localhost:8080/api/v1/system/status
```

### 4. Compilar y ejecutar las pruebas

No es necesario instalar Maven globalmente. El proyecto incluye Maven Wrapper:

```powershell
.\mvnw.cmd test
```

En Linux o macOS:

```bash
./mvnw test
```

### 5. Verificar arquitectura modular

El comando anterior ejecuta `ModularityTest`. La prueba fallará si se introducen dependencias cíclicas o accesos inválidos entre módulos.

## Estado actual

Este ZIP corresponde a la **Fase 0 — esqueleto del backend**. Incluye la estructura modular, configuración, PostgreSQL/PostGIS local, Flyway, seguridad local/productiva, respuestas API, correlación de solicitudes, Dockerfile y despliegue base en Render.

Todavía no incluye tablas de empresa, usuarios ni animales. El siguiente paso es crear `V3__create_core.sql` y desarrollar verticalmente el módulo `empresas`.
