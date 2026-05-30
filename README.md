# ClassTrack — Backend API

Sistema de gestión de asistencia académica mediante códigos QR y geolocalización.

## Stack

| Capa | Tecnología |
|---|---|
| Lenguaje | Java 17 |
| Framework | Spring Boot 4.0.6 |
| Base de datos | PostgreSQL 16 |
| Autenticación | JWT (HS256, 24h) |
| Contenedor | Docker (Alpine JRE 17) |
| Infraestructura | AWS ECS Fargate + RDS + ECR |
| IaC | AWS CloudFormation |
| Documentación | OpenAPI 3 / Swagger UI |

---

## Arquitectura

```
Mobile App
    │
    ▼
ECS Fargate (Spring Boot)
    │
    ├── RDS PostgreSQL (classtrack DB)
    └── ECR (imagen Docker)
```

### Roles

- **TEACHER** — crea cursos, gestiona estudiantes, abre/cierra sesiones de asistencia, consulta reportes.
- **STUDENT** — consulta cursos inscritos, registra asistencia por QR, consulta historial.

---

## Estructura del proyecto

```
src/main/java/com/mgads/appmoviles/classtrack/
├── auth/               # Registro, login, logout
├── profile/            # Perfil y QR de identificación
├── courses/            # CRUD de cursos
├── students/           # Inscripción de estudiantes en cursos
├── attendance/         # Sesiones y registros de asistencia
├── reports/            # Reportes agregados
├── security/           # JWT, filtros, blacklist de tokens
├── common/             # ApiResponse, excepciones globales
└── exception/          # Excepciones personalizadas
```

---

## Base de datos

```
users
 └── courses (teacher_id → users.id)
      └── course_students (course_id, student_id → users.id)
      └── attendance_sessions (course_id)
           └── attendance_records (session_id, student_id)
```

### Tablas

| Tabla | Descripción |
|---|---|
| `users` | Docentes y estudiantes con rol TEACHER/STUDENT |
| `courses` | Cursos asociados a un docente |
| `course_students` | Relación muchos-a-muchos cursos/estudiantes |
| `attendance_sessions` | Sesiones QR con expiración de 15 minutos |
| `attendance_records` | Registro de asistencia con coordenadas GPS |

---

## Endpoints

Base URL: `/api/v1`

### Auth
| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/auth/register` | Público | Registrar usuario |
| POST | `/auth/login` | Público | Login, retorna JWT |
| POST | `/auth/logout` | Autenticado | Invalida el token actual |

### Perfil
| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/profile` | Autenticado | Datos del usuario autenticado |
| GET | `/profile/digital-id` | Autenticado | QR de identificación |

### Cursos
| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/courses` | TEACHER | Crear curso |
| GET | `/courses` | Autenticado | Listar cursos (filtrado por rol) |
| DELETE | `/courses/{id}` | TEACHER | Eliminar curso propio |

### Estudiantes en cursos
| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/courses/{id}/students` | TEACHER | Inscribir estudiante |
| GET | `/courses/{id}/students` | TEACHER | Listar estudiantes del curso |
| DELETE | `/courses/{id}/students/{studentId}` | TEACHER | Desvincular estudiante |

### Sesiones de asistencia
| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/attendance/sessions` | TEACHER | Abrir sesión QR (expira en 15 min) |
| GET | `/attendance/sessions` | TEACHER | Listar sesiones de un curso |
| POST | `/attendance/sessions/{id}/close` | TEACHER | Cerrar sesión manualmente |

### Asistencia
| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/attendance/checkin` | STUDENT | Registrar asistencia con QR y GPS |
| GET | `/attendance/history` | STUDENT | Historial de asistencias del estudiante |
| GET | `/attendance/sessions/{id}/records` | TEACHER | Registros de una sesión |

### Reportes
| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/reports/sessions/{id}` | TEACHER | Reporte de una sesión |
| GET | `/reports/courses/{id}` | TEACHER | Reporte de un curso |
| GET | `/reports/students/{id}` | TEACHER | Reporte de un estudiante |

---

## Variables de entorno

| Variable | Default | Descripción |
|---|---|---|
| `DB_HOST` | `localhost` | Host de PostgreSQL |
| `DB_PORT` | `5432` | Puerto de PostgreSQL |
| `DB_NAME` | `classtrack` | Nombre de la base de datos |
| `DB_USER` | `postgres` | Usuario de BD |
| `DB_PASSWORD` | `postgres` | Contraseña de BD |
| `JWT_SECRET` | *(valor por defecto)* | Clave HS256 (mínimo 32 caracteres) |
| `JWT_EXPIRATION` | `86400000` | Expiración del JWT en ms (24h) |
| `SESSION_DURATION_MINUTES` | `15` | Duración del QR de asistencia |

---

## Ejecución local

### Requisitos
- Java 17
- Maven 3.9+
- Docker y Docker Compose

### Con Docker Compose

```bash
docker compose up --build
```

Inicia PostgreSQL 16 y el API en `http://localhost:8080`.

El schema se ejecuta automáticamente desde `src/main/resources/schema.sql`.

### Sin Docker

```bash
# Iniciar PostgreSQL local primero, luego:
mvn spring-boot:run
```

### Swagger UI

```
http://localhost:8080/swagger-ui.html
```

---

## Despliegue en AWS

### Prerequisitos
- AWS CLI configurado (`aws configure`)
- Docker instalado
- Permisos IAM: CloudFormation, ECS, ECR, RDS, VPC, IAM

### Pasos

**1. Desplegar infraestructura y aplicación**

En Windows:
```bash
cloudformation\deploy.bat
```

En Linux/Mac:
```bash
bash cloudformation/deploy.sh
```

El script ejecuta 5 pasos automatizados:
1. Crea VPC, subnets, security groups, ECR, RDS (`classtrack-infra`)
2. Obtiene los outputs del stack de infraestructura
3. Construye y sube la imagen Docker a ECR
4. Ejecuta `schema.sql` en RDS
5. Crea el cluster ECS y servicio Fargate (`classtrack-app`)

**2. Obtener la IP pública**

```bash
# Listar tareas
aws ecs list-tasks --cluster classtrack-cluster --region us-east-1

# Obtener ENI de la tarea
aws ecs describe-tasks --cluster classtrack-cluster --region us-east-1 \
  --tasks <TASK_ARN> --query 'tasks[0].attachments[0].details'

# Obtener IP pública
aws ec2 describe-network-interfaces \
  --network-interface-ids <ENI_ID> --region us-east-1 \
  --query 'NetworkInterfaces[0].Association.PublicIp' --output text
```

**3. Acceder a la API**

```
http://<PUBLIC_IP>:8080/swagger-ui.html
http://<PUBLIC_IP>:8080/api/v1/...
```

### Eliminar todos los recursos AWS

```bash
bash cloudformation/destroy.sh
```

---

## Colección Postman

El archivo `ClassTrack.postman_collection.json` contiene 27 requests organizados en 7 carpetas con scripts que guardan automáticamente los tokens JWT tras el login.

**Importar:** Postman → Import → seleccionar el archivo.

**Variable a configurar:** `baseUrl` → `http://<PUBLIC_IP>:8080/api/v1`

---

## Seguridad

- Autenticación stateless con JWT (Bearer token)
- Tokens invalidados en memoria tras logout (`TokenBlacklistService`)
- Contraseñas hasheadas con BCrypt
- Validación de roles con Spring Security (`@PreAuthorize`)
- Validación de entrada con Jakarta Bean Validation en todos los DTOs
