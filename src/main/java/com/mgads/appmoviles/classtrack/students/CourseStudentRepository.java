package com.mgads.appmoviles.classtrack.students;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseStudentRepository extends JpaRepository<CourseStudentEntity, Long> {

    boolean existsByCourseIdAndStudentId(Long courseId, Long studentId);

    void deleteByCourseIdAndStudentId(Long courseId, Long studentId);

    @Query("""
            SELECT cs.student.id, cs.student.fullName, cs.student.studentCode
            FROM CourseStudentEntity cs
            WHERE cs.course.id = :courseId
            """)
    List<Object[]> findStudentsByCourseId(@Param("courseId") Long courseId);
}
