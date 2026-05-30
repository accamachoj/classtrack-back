package com.mgads.appmoviles.classtrack.courses;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseRepository extends JpaRepository<CourseEntity, Long> {

    List<CourseEntity> findByTeacherIdAndActiveTrue(Long teacherId);

    @Query("""
            SELECT c FROM CourseEntity c
            JOIN CourseStudentEntity cs ON cs.course.id = c.id
            WHERE cs.student.id = :studentId AND c.active = true
            """)
    List<CourseEntity> findByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT COUNT(cs) FROM CourseStudentEntity cs WHERE cs.course.id = :courseId")
    long countStudentsByCourseId(@Param("courseId") Long courseId);
}
