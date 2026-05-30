-- ClassTrack Database Schema
-- PostgreSQL 16+

CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL     PRIMARY KEY,
    full_name     VARCHAR(150)  NOT NULL,
    email         VARCHAR(255)  NOT NULL,
    password_hash VARCHAR(255)  NOT NULL,
    role          VARCHAR(20)   NOT NULL,
    student_code  VARCHAR(50)   NULL,
    active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('TEACHER', 'STUDENT'))
);

CREATE TABLE IF NOT EXISTS courses (
    id          BIGSERIAL     PRIMARY KEY,
    teacher_id  BIGINT        NOT NULL,
    name        VARCHAR(120)  NOT NULL,
    description TEXT          NULL,
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_courses_teacher FOREIGN KEY (teacher_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS course_students (
    id         BIGSERIAL  PRIMARY KEY,
    course_id  BIGINT     NOT NULL,
    student_id BIGINT     NOT NULL,
    created_at TIMESTAMP  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_course_students_course  FOREIGN KEY (course_id)  REFERENCES courses (id),
    CONSTRAINT fk_course_students_student FOREIGN KEY (student_id) REFERENCES users (id),
    CONSTRAINT uq_course_students         UNIQUE (course_id, student_id)
);

CREATE TABLE IF NOT EXISTS attendance_sessions (
    id         BIGSERIAL     PRIMARY KEY,
    course_id  BIGINT        NOT NULL,
    qr_token   VARCHAR(255)  NOT NULL,
    status     VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    started_at TIMESTAMP     NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP     NOT NULL,
    closed_at  TIMESTAMP     NULL,
    created_at TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_attendance_sessions_course FOREIGN KEY (course_id) REFERENCES courses (id),
    CONSTRAINT chk_attendance_sessions_status CHECK (status IN ('ACTIVE', 'CLOSED'))
);

CREATE TABLE IF NOT EXISTS attendance_records (
    id            BIGSERIAL      PRIMARY KEY,
    session_id    BIGINT         NOT NULL,
    student_id    BIGINT         NOT NULL,
    latitude      NUMERIC(10, 7) NOT NULL,
    longitude     NUMERIC(10, 7) NOT NULL,
    registered_at TIMESTAMP      NOT NULL DEFAULT NOW(),
    created_at    TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_attendance_records_session FOREIGN KEY (session_id) REFERENCES attendance_sessions (id),
    CONSTRAINT fk_attendance_records_student FOREIGN KEY (student_id) REFERENCES users (id),
    CONSTRAINT uq_attendance_records         UNIQUE (session_id, student_id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_users_email                  ON users (email);
CREATE INDEX IF NOT EXISTS idx_courses_teacher              ON courses (teacher_id);
CREATE INDEX IF NOT EXISTS idx_course_students_course       ON course_students (course_id);
CREATE INDEX IF NOT EXISTS idx_course_students_student      ON course_students (student_id);
CREATE INDEX IF NOT EXISTS idx_attendance_sessions_course   ON attendance_sessions (course_id);
CREATE INDEX IF NOT EXISTS idx_attendance_records_session   ON attendance_records (session_id);
CREATE INDEX IF NOT EXISTS idx_attendance_records_student   ON attendance_records (student_id);
