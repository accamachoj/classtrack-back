package com.mgads.appmoviles.classtrack.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecordEntity, Long> {

    boolean existsBySessionIdAndStudentId(Long sessionId, Long studentId);

    @Query("""
            SELECT ar.student.id, ar.student.fullName, ar.registeredAt
            FROM AttendanceRecordEntity ar
            WHERE ar.session.id = :sessionId
            """)
    List<Object[]> findRecordsBySessionId(@Param("sessionId") Long sessionId);

    long countBySessionId(Long sessionId);

    @Query("""
            SELECT ar FROM AttendanceRecordEntity ar
            WHERE ar.student.id = :studentId
            """)
    List<AttendanceRecordEntity> findByStudentId(@Param("studentId") Long studentId);

    @Query("""
            SELECT COUNT(ar) FROM AttendanceRecordEntity ar
            WHERE ar.session.id = :sessionId AND ar.student.id = :studentId
            """)
    long countBySessionIdAndStudentId(@Param("sessionId") Long sessionId, @Param("studentId") Long studentId);
}
