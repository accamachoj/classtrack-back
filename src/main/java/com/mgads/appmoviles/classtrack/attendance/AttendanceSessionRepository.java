package com.mgads.appmoviles.classtrack.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSessionEntity, Long> {

    Optional<AttendanceSessionEntity> findByQrToken(String qrToken);

    List<AttendanceSessionEntity> findByCourseId(Long courseId);

    @Query("""
            SELECT s FROM AttendanceSessionEntity s
            JOIN CourseStudentEntity cs ON cs.course.id = s.course.id
            WHERE cs.student.id = :studentId
            """)
    List<AttendanceSessionEntity> findSessionsByStudentId(@Param("studentId") Long studentId);

    long countByCourseId(Long courseId);
}
