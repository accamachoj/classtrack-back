# ClassTrack — Plan de Desarrollo Backend

## Resumen del Contexto Extraído

- **Proyecto:** Aplicación móvil Android para gestión de asistencia académica mediante QR y geolocalización.
- **Stack backend:** Spring Boot 4.0.6, Java 17, Maven, PostgreSQL 16+, Spring Security + JWT, Swagger/OpenAPI.
- **Infraestructura:** Docker, Railway.
- **Roles:** TEACHER y STUDENT.
- **Base de datos:** 5 tablas — `users`, `courses`, `course_students`, `attendance_sessions`, `attendance_records`.
- **Paquete base:** `com.mgads.appmoviles.classtrack`
- **Dependencias actuales en pom.xml:** spring-boot-starter-data-jpa, spring-boot-starter-webmvc, lombok, devtools. **Faltan:** spring-boot-starter-security, spring-boot-starter-validation, jjwt, postgresql driver, springdoc-openapi.
- **API base URL:** `/api/v1`
- **Formato de respuesta estándar:** `{ "success": bool, "message": string, "data": object }`

---

## Ambiguedades Detectadas — Estado de Resolución

| # | Ambiguedad | Decisión tomada | Estado |
|---|-----------|----------------|--------|
| 1 | `student_code` — obligatorio para estudiantes pero no estaba en el request de registro | **Generarlo automáticamente** como `CT{timestamp}` al registrar un STUDENT | ✅ Resuelto — implementado en Fase 3 |
| 2 | Stack: doc dice Spring Boot 3 / Java 21, pom.xml tiene Spring Boot 4.0.6 / Java 17 | **Mantener pom.xml** existente (Spring Boot 4.0.6 / Java 17) | ✅ Resuelto — no requiere cambio |
| 3 | QR de asistencia — tiempo de expiración no especificado | **15 minutos** de duración por sesión | ⚠️ Requiere ajuste — actualmente 30 min. Ver Fase 12 |
| 4 | GPS — sin validación de rango definida | **Validar rangos**: latitud [-90, 90], longitud [-180, 180] | ⚠️ Requiere implementación. Ver Fase 12 |
| 5 | GET `/courses` — filtrado por rol no especificado | **Filtrado automático por rol**: teacher ve sus cursos, student ve los suyos | ✅ Resuelto — implementado en Fase 5 |

**Funcionalidad faltante detectada:**
- Historial de asistencia del estudiante (`GET /api/v1/attendance/history`) — está en el alcance del proyecto charter pero no tenía endpoint definido. Ver Fase 13.

---

## Fase 0 — Configuración del Proyecto

**Duración estimada:** 1-2 horas

### Objetivo
Preparar el proyecto Spring Boot con todas las dependencias y configuración base.

### Actividades

1. **Agregar dependencias faltantes al `pom.xml`:**
   - `spring-boot-starter-security`
   - `spring-boot-starter-validation`
   - `org.postgresql:postgresql` (runtime)
   - `io.jsonwebtoken:jjwt-api`, `jjwt-impl`, `jjwt-jackson`
   - `org.springdoc:springdoc-openapi-starter-webmvc-ui`

2. **Crear `application.properties` / `application.yml`:**
   - Datasource PostgreSQL (variables de entorno: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`)
   - JWT config (`JWT_SECRET`, `JWT_EXPIRATION`)
   - Swagger habilitado
   - JPA ddl-auto: validate (usar script DDL manual)
   - Puerto: 8080

3. **Crear estructura de paquetes:**
   ```
   com.mgads.appmoviles.classtrack
   ├── auth/
   ├── users/
   ├── courses/
   ├── students/
   ├── attendance/
   ├── reports/
   ├── common/
   ├── security/
   ├── config/
   └── exception/
   ```

4. **Crear clases base:**
   - `ApiResponse<T>` — wrapper estándar de respuesta
   - `GlobalExceptionHandler` — `@RestControllerAdvice`
   - Excepciones: `NotFoundException`, `BadRequestException`, `UnauthorizedException`, `ConflictException`

### Entregable
Proyecto compila y arranca sin errores (sin endpoints aún).

---

## Fase 1 — Script DDL de Base de Datos

**Duración estimada:** 1 hora

### Objetivo
Crear el script SQL completo para PostgreSQL.

### Tablas a crear (en orden)

| Tabla | Columnas clave | FK | Constraints |
|-------|---------------|----|----|
| `users` | id, full_name, email, password_hash, role, student_code, active, created_at, updated_at | — | UNIQUE(email), idx_users_email |
| `courses` | id, teacher_id, name, description, active, created_at, updated_at | teacher_id → users(id) | idx_courses_teacher |
| `course_students` | id, course_id, student_id, created_at | course_id → courses(id), student_id → users(id) | UNIQUE(course_id, student_id) |
| `attendance_sessions` | id, course_id, qr_token, status, started_at, expires_at, closed_at, created_at | course_id → courses(id) | idx_attendance_sessions_course |
| `attendance_records` | id, session_id, student_id, latitude, longitude, registered_at, created_at | session_id → attendance_sessions(id), student_id → users(id) | UNIQUE(session_id, student_id) |

### Índices adicionales
- `idx_course_students_course` ON course_students(course_id)
- `idx_course_students_student` ON course_students(student_id)
- `idx_attendance_records_session` ON attendance_records(session_id)
- `idx_attendance_records_student` ON attendance_records(student_id)

### Entregable
Archivo `schema.sql` ejecutable en PostgreSQL limpio.

---

## Fase 2 — Entidades JPA + Seguridad JWT

**Duración estimada:** 3-4 horas

### Objetivo
Mapear las 5 tablas a entidades JPA e implementar la capa de seguridad.

### Entidades

| Entidad | Tabla | Campos de auditoría |
|---------|-------|-------------------|
| `UserEntity` | users | created_at, updated_at |
| `CourseEntity` | courses | created_at, updated_at |
| `CourseStudentEntity` | course_students | created_at |
| `AttendanceSessionEntity` | attendance_sessions | created_at |
| `AttendanceRecordEntity` | attendance_records | created_at |

### Seguridad

| Componente | Responsabilidad |
|-----------|----------------|
| `SecurityConfig` | Configuración de filtros, rutas públicas vs protegidas |
| `JwtService` | Generar token, validar token, extraer claims (userId, email, role) |
| `JwtAuthenticationFilter` | Interceptar requests, validar Bearer token |
| `UserDetailsServiceImpl` | Cargar usuario desde BD para Spring Security |

### Rutas públicas (sin JWT)
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `/swagger-ui/**`, `/v3/api-docs/**`

### Entregable
Aplicación arranca con seguridad JWT configurada. Todas las rutas protegidas retornan 401 sin token.

---

## Fase 3 — Módulo Auth (Registro + Login)

**Duración estimada:** 2-3 horas

### Objetivo
Implementar registro de usuarios e inicio de sesión.

### Componentes

| Componente | Tipo |
|-----------|------|
| `AuthController` | @RestController — `/api/v1/auth` |
| `AuthService` | @Service |
| `UserRepository` | @Repository — JpaRepository |
| `RegisterRequest` | DTO — fullName, email, password, role |
| `LoginRequest` | DTO — email, password |
| `AuthResponse` | DTO — token, userId, role |

### Endpoints

| Método | Ruta | Propósito | Auth |
|--------|------|----------|------|
| POST | `/api/v1/auth/register` | Crear cuenta nueva | No |
| POST | `/api/v1/auth/login` | Iniciar sesión | No |

### Validaciones
- `fullName`: @NotBlank, @Size(max=150)
- `email`: @NotBlank, @Email, único en BD
- `password`: @NotBlank, @Size(min=8)
- `role`: @NotNull, solo TEACHER o STUDENT
- Login: verificar que email existe y password coincide con BCrypt

### Lógica de negocio
- Registro: hashear password con BCrypt → guardar usuario → generar JWT → retornar token + userId
- Login: buscar por email → verificar password → generar JWT → retornar token + userId + role

### Entregable
Un usuario puede registrarse y hacer login. Recibe JWT funcional.

---

## Fase 4 — Módulo Profile + Identificación Digital

**Duración estimada:** 1-2 horas

### Objetivo
Permitir consultar perfil y obtener datos para QR digital.

### Componentes

| Componente | Tipo |
|-----------|------|
| `ProfileController` | @RestController — `/api/v1/profile` |
| `UserService` | @Service |
| `UserResponse` | DTO — id, fullName, email, role |
| `DigitalIdResponse` | DTO — userId, fullName, studentCode, qrContent |

### Endpoints

| Método | Ruta | Propósito | Auth | Rol |
|--------|------|----------|------|-----|
| GET | `/api/v1/profile` | Obtener perfil del usuario autenticado | Sí | Ambos |
| GET | `/api/v1/profile/digital-id` | Obtener datos para QR de identificación | Sí | Ambos |

### Lógica
- Extraer userId del JWT para obtener perfil
- `qrContent` se genera como: `"CT_USER_{userId}"`

### Entregable
Usuario autenticado puede consultar su perfil y obtener su identificación digital.

---

## Fase 5 — Módulo Courses (CRUD)

**Duración estimada:** 2-3 horas

### Objetivo
Permitir a docentes gestionar cursos.

### Componentes

| Componente | Tipo |
|-----------|------|
| `CourseController` | @RestController — `/api/v1/courses` |
| `CourseService` | @Service |
| `CourseRepository` | @Repository |
| `CreateCourseRequest` | DTO — name, description |
| `CourseResponse` | DTO — id, name, description, studentCount |
| `CourseListResponse` | DTO — id, name, studentCount |

### Endpoints

| Método | Ruta | Propósito | Auth | Rol |
|--------|------|----------|------|-----|
| POST | `/api/v1/courses` | Crear curso | Sí | TEACHER |
| GET | `/api/v1/courses` | Listar cursos | Sí | Ambos |
| GET | `/api/v1/courses/{courseId}` | Detalle de curso | Sí | Ambos |
| DELETE | `/api/v1/courses/{courseId}` | Eliminar curso (soft delete) | Sí | TEACHER |

### Validaciones
- `name`: @NotBlank, @Size(max=120)
- Solo el teacher propietario puede eliminar su curso
- GET `/courses` filtra: teacher ve sus cursos, student ve cursos donde está inscrito

### Entregable
CRUD de cursos funcional con control de acceso por rol.

---

## Fase 6 — Módulo Students (Vinculación)

**Duración estimada:** 2 horas

### Objetivo
Permitir a docentes vincular/desvincular estudiantes a cursos.

### Componentes

| Componente | Tipo |
|-----------|------|
| `CourseStudentController` | @RestController — `/api/v1/courses/{courseId}/students` |
| `CourseStudentService` | @Service |
| `CourseStudentRepository` | @Repository |
| `LinkStudentRequest` | DTO — studentId |
| `StudentResponse` | DTO — id, fullName, studentCode |

### Endpoints

| Método | Ruta | Propósito | Auth | Rol |
|--------|------|----------|------|-----|
| POST | `/api/v1/courses/{courseId}/students` | Vincular estudiante | Sí | TEACHER |
| GET | `/api/v1/courses/{courseId}/students` | Listar estudiantes del curso | Sí | TEACHER |
| DELETE | `/api/v1/courses/{courseId}/students/{studentId}` | Desvincular estudiante | Sí | TEACHER |

### Validaciones
- Solo el teacher propietario del curso puede vincular/desvincular
- El studentId debe corresponder a un usuario con rol STUDENT
- No permitir duplicados (UNIQUE constraint en BD)
- Verificar que el curso existe y está activo

### Entregable
Docente puede vincular estudiantes a cursos mediante su userId (obtenido del QR digital).

---

## Fase 7 — Módulo Attendance Sessions

**Duración estimada:** 2-3 horas

### Objetivo
Permitir a docentes crear sesiones de asistencia y generar QR temporal.

### Componentes

| Componente | Tipo |
|-----------|------|
| `AttendanceSessionController` | @RestController — `/api/v1/attendance/sessions` |
| `AttendanceSessionService` | @Service |
| `AttendanceSessionRepository` | @Repository |
| `CreateSessionRequest` | DTO — courseId |
| `SessionResponse` | DTO — sessionId, courseId, qrToken, expiresAt |
| `SessionDetailResponse` | DTO — sessionId, courseId, status |

### Endpoints

| Método | Ruta | Propósito | Auth | Rol |
|--------|------|----------|------|-----|
| POST | `/api/v1/attendance/sessions` | Crear sesión de asistencia | Sí | TEACHER |
| GET | `/api/v1/attendance/sessions/{sessionId}` | Obtener detalle de sesión | Sí | TEACHER |
| POST | `/api/v1/attendance/sessions/{sessionId}/close` | Cerrar sesión | Sí | TEACHER |

### Lógica
- Al crear sesión: generar `qrToken` único (UUID), calcular `expiresAt`, estado ACTIVE
- Al cerrar: cambiar estado a CLOSED, registrar `closed_at`
- Solo el teacher del curso asociado puede gestionar la sesión

### Entregable
Docente puede crear sesiones, obtener el token QR y cerrar sesiones.

---

## Fase 8 — Módulo Attendance Check-in

**Duración estimada:** 2 horas

### Objetivo
Permitir a estudiantes registrar asistencia.

### Componentes

| Componente | Tipo |
|-----------|------|
| `AttendanceController` | @RestController — `/api/v1/attendance` |
| `AttendanceService` | @Service |
| `AttendanceRecordRepository` | @Repository |
| `CheckinRequest` | DTO — sessionId, latitude, longitude |
| `CheckinResponse` | DTO — attendanceId, registeredAt |
| `AttendanceRecordResponse` | DTO — studentId, studentName, registeredAt |

### Endpoints

| Método | Ruta | Propósito | Auth | Rol |
|--------|------|----------|------|-----|
| POST | `/api/v1/attendance/checkin` | Registrar asistencia | Sí | STUDENT |
| GET | `/api/v1/attendance/sessions/{sessionId}/records` | Listar asistentes de sesión | Sí | TEACHER |

### Validaciones
- La sesión debe estar ACTIVE y no expirada
- El estudiante debe estar vinculado al curso de la sesión
- Un estudiante solo puede registrar asistencia una vez por sesión (UNIQUE constraint)
- Latitude: @NotNull, Longitude: @NotNull

### Datos almacenados
- session_id, student_id (del JWT), latitude, longitude, registered_at (timestamp actual)

### Entregable
Estudiante puede registrar asistencia con geolocalización. Docente puede consultar asistentes.

---

## Fase 9 — Módulo Reports

**Duración estimada:** 2 horas

### Objetivo
Exponer consultas agregadas de asistencia.

### Componentes

| Componente | Tipo |
|-----------|------|
| `ReportController` | @RestController — `/api/v1/reports` |
| `ReportService` | @Service |
| `SessionReportResponse` | DTO — sessionId, totalStudents, attendees, absent |
| `CourseReportResponse` | DTO — courseId, courseName, totalStudents, attendanceRate |
| `StudentReportResponse` | DTO — studentId, studentName, attendancePercentage, sessionsAttended |

### Endpoints

| Método | Ruta | Propósito | Auth | Rol |
|--------|------|----------|------|-----|
| GET | `/api/v1/reports/sessions/{sessionId}` | Reporte de una sesión | Sí | TEACHER |
| GET | `/api/v1/reports/courses/{courseId}` | Reporte general del curso | Sí | TEACHER |
| GET | `/api/v1/reports/students/{studentId}` | Reporte de un estudiante | Sí | Ambos |

### Lógica
- **Sesión:** total estudiantes del curso vs registros de asistencia de la sesión → asistentes y ausentes
- **Curso:** promedio de asistencia sobre todas las sesiones del curso
- **Estudiante:** porcentaje de sesiones a las que asistió vs sesiones totales de sus cursos

### Entregable
Todos los reportes definidos en los contratos API funcionan correctamente.

---

## Fase 10 — Swagger + Validación Final

**Duración estimada:** 1 hora

### Objetivo
Documentar todos los endpoints y verificar que los contratos coincidan con `04-api-contracts.md`.

### Actividades
- Agregar anotaciones OpenAPI a todos los controllers (`@Operation`, `@ApiResponse`, `@Tag`)
- Verificar que `/swagger-ui.html` sea accesible
- Probar todos los endpoints manualmente desde Swagger
- Verificar formato de respuesta estándar en todos los endpoints

### Entregable
Swagger accesible con todos los endpoints documentados.

---

## Fase 11 — Docker + Railway

**Duración estimada:** 1-2 horas

### Objetivo
Contenerizar y desplegar.

### Actividades

1. **Dockerfile:**
   - Multi-stage build (Maven build + JRE runtime)
   - Puerto 8080 expuesto

2. **docker-compose.yml:**
   - Servicio `api` (Spring Boot)
   - Servicio `db` (PostgreSQL 16)
   - Variables de entorno configuradas

3. **Railway:**
   - Crear proyecto con servicio PostgreSQL
   - Configurar variables de entorno
   - Desplegar desde GitHub

### Verificación
- `docker build .` exitoso
- `docker compose up` levanta API + BD
- URL pública de Railway responde
- Swagger accesible desde URL pública

### Entregable
Backend desplegado y accesible públicamente.

---

---

## Fase 12 — Correcciones por Ambiguedades Resueltas

**Duración estimada:** 1 hora

### Objetivo
Aplicar los ajustes derivados de las decisiones tomadas sobre las ambiguedades 3 y 4.

### Actividades

#### 12.1 — Ajuste de expiración QR (Ambiguedad 3)
- Cambiar la constante `SESSION_DURATION_MINUTES` de `30` a `15` en `AttendanceSessionService`
- Hacer la duración configurable via `application.properties` con la propiedad `app.session.duration-minutes`

**Archivo:** `AttendanceSessionService.java`
**Propiedad a agregar:** `app.session.duration-minutes=15`

#### 12.2 — Validación de coordenadas GPS (Ambiguedad 4)
- Agregar validaciones de rango en `CheckinRequest`:
  - `latitude`: entre -90.0 y 90.0 usando `@DecimalMin` y `@DecimalMax`
  - `longitude`: entre -180.0 y 180.0 usando `@DecimalMin` y `@DecimalMax`

**Archivo:** `CheckinRequest.java`

### Entregable
- QR expira en 15 minutos
- Coordenadas GPS con rangos válidos validados por Bean Validation

---

## Fase 13 — Historial de Asistencia del Estudiante

**Duración estimada:** 1 hora

### Objetivo
Permitir al estudiante autenticado consultar su propio historial de asistencias. Funcionalidad contemplada en el project charter pero sin endpoint definido en los contratos originales.

### Componentes

| Componente | Tipo |
|-----------|------|
| `AttendanceHistoryResponse` | DTO — sessionId, courseName, registeredAt, latitude, longitude |

### Endpoint

| Método | Ruta | Propósito | Auth | Rol |
|--------|------|----------|------|-----|
| GET | `/api/v1/attendance/history` | Historial propio del estudiante | Sí | STUDENT |

### Lógica
- Extraer `studentId` del JWT
- Consultar todos los `AttendanceRecordEntity` del estudiante
- Retornar lista con: `sessionId`, `courseName`, `registeredAt`, `latitude`, `longitude`

### Validaciones
- Solo el estudiante autenticado puede ver su propio historial (se obtiene del JWT, no del path)

### Entregable
Estudiante puede consultar todas sus asistencias registradas con detalle de curso, fecha y ubicación.

---

---

## Fase 14 — Cierre de Sesión (Logout)

**Duración estimada:** 1 hora

### Análisis

JWT es stateless por diseño — el servidor no guarda sesiones. Esto significa que invalidar un token requiere una estrategia explícita en el backend. Sin ella, un token sigue siendo válido hasta su expiración (24h) incluso después de que el usuario cierre sesión.

### Estrategia elegida — Token Blacklist en memoria

Para un proyecto académico de instancia única, se usa un `HashSet<String>` en memoria que almacena los tokens invalidados. El `JwtAuthenticationFilter` verifica contra esta lista en cada request.

**Limitación conocida:** Si la aplicación se reinicia, la blacklist se pierde. Los tokens previamente invalidados volverán a ser válidos hasta su expiración. Aceptable para alcance académico.

### Componentes

| Componente | Tipo | Responsabilidad |
|-----------|------|----------------|
| `TokenBlacklistService` | `@Service` — `security/` | Almacena y consulta tokens invalidados |
| `AuthController` | Modificación | Agregar `POST /auth/logout` |
| `JwtAuthenticationFilter` | Modificación | Verificar blacklist antes de autenticar |

### Endpoint

| Método | Ruta | Propósito | Auth | Rol |
|--------|------|----------|------|-----|
| POST | `/api/v1/auth/logout` | Invalidar el token JWT actual | Sí | Ambos |

### Request

Sin body — el token se extrae del header `Authorization: Bearer <token>`.

### Response

```json
{ "success": true, "message": "Logged out successfully" }
```

### Lógica

1. `AuthController.logout()` extrae el token del header `Authorization`
2. Llama a `TokenBlacklistService.invalidate(token)`
3. El token queda almacenado en el `HashSet`
4. En cada request subsiguiente, `JwtAuthenticationFilter` consulta `TokenBlacklistService.isBlacklisted(token)`
5. Si está en la blacklist → retorna 401 sin procesar el request

### Entregable
- `POST /api/v1/auth/logout` retorna 200 y el token queda inválido inmediatamente
- Requests posteriores con el mismo token retornan 401

---

## Resumen de Fases

| Fase | Módulo | Endpoints | Horas Est. | Estado |
|------|--------|-----------|-----------|--------|
| 0 | Configuración proyecto | 0 | 1-2h | ✅ Completado |
| 1 | Script DDL | 0 | 1h | ✅ Completado |
| 2 | Entidades JPA + JWT | 0 | 3-4h | ✅ Completado |
| 3 | Auth (Register + Login) | 2 | 2-3h | ✅ Completado |
| 4 | Profile + Digital ID | 2 | 1-2h | ✅ Completado |
| 5 | Courses CRUD | 4 | 2-3h | ✅ Completado |
| 6 | Student Linking | 3 | 2h | ✅ Completado |
| 7 | Attendance Sessions | 3 | 2-3h | ✅ Completado |
| 8 | Attendance Check-in | 2 | 2h | ✅ Completado |
| 9 | Reports | 3 | 2h | ✅ Completado |
| 10 | Swagger | 0 | 1h | ✅ Completado |
| 11 | Docker + Railway | 0 | 1-2h | ✅ Completado |
| 12 | Correcciones ambiguedades (QR 15min + GPS) | 0 | 1h | ✅ Completado |
| 13 | Historial asistencia estudiante | 1 | 1h | ✅ Completado |
| 14 | Logout (token blacklist) | 1 | 1h | ⬜ Pendiente |
| **Total** | | **22 endpoints** | **~24-30h** | |

---

## Orden de Ejecución Recomendado

```
Fase 0 → Fase 1 → Fase 2 → Fase 3 → Fase 4
                                        ↓
                              Fase 5 → Fase 6 → Fase 7 → Fase 8 → Fase 9
                                                                      ↓
                                                   Fase 10 → Fase 12 → Fase 13 → Fase 11
```

Cada fase depende de la anterior. No saltar fases. Cada fase debe probarse antes de avanzar a la siguiente.
