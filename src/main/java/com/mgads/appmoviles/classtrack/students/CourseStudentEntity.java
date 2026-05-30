package com.mgads.appmoviles.classtrack.students;

import com.mgads.appmoviles.classtrack.courses.CourseEntity;
import com.mgads.appmoviles.classtrack.users.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "course_students",
    uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "student_id"})
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class CourseStudentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private CourseEntity course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private UserEntity student;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
