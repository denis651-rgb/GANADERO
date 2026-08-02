# GANADERO Backend — Fase 1

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

La Fase 0 está terminada y la Fase 1 se encuentra en desarrollo. Ya están implementados los fundamentos
transversales, empresa/configuración, seguridad empresarial, propiedades, potreros, animales y sus catálogos.

Endpoints disponibles:

```text
GET   /api/v1/empresa
PATCH /api/v1/empresa
GET   /api/v1/empresa/configuracion
PATCH /api/v1/empresa/configuracion

GET   /api/v1/auth/me
GET   /api/v1/auth/permisos
GET   /api/v1/usuarios
POST  /api/v1/usuarios
GET   /api/v1/usuarios/{id}
PATCH /api/v1/usuarios/{id}
POST  /api/v1/usuarios/{id}/bloquear
POST  /api/v1/usuarios/{id}/activar
PUT   /api/v1/usuarios/{id}/roles
PUT   /api/v1/usuarios/{id}/propiedades

GET   /api/v1/roles
POST  /api/v1/roles
GET   /api/v1/roles/{id}
PATCH /api/v1/roles/{id}
PUT   /api/v1/roles/{id}/permisos
GET   /api/v1/roles/permisos

GET   /api/v1/propiedades
POST  /api/v1/propiedades
GET   /api/v1/propiedades/{id}
PATCH /api/v1/propiedades/{id}
GET   /api/v1/propiedades/{id}/sectores
POST  /api/v1/propiedades/{id}/sectores
PATCH /api/v1/sectores/{id}

GET   /api/v1/tipos-pasto
GET   /api/v1/potreros
POST  /api/v1/potreros
GET   /api/v1/potreros/{id}
PATCH /api/v1/potreros/{id}

GET   /api/v1/animales
POST  /api/v1/animales
GET   /api/v1/animales/{id}
PATCH /api/v1/animales/{id}
PATCH /api/v1/animales/{id}/estado
GET   /api/v1/razas
GET   /api/v1/categorias-animal
```

El perfil `local` carga una empresa, un propietario y las asignaciones necesarias para desarrollo. Estos datos
no se incluyen en producción. Las credenciales y sesiones siguen siendo responsabilidad de Supabase Auth;
el backend solo almacena perfiles empresariales, membresías y autorizaciones.
