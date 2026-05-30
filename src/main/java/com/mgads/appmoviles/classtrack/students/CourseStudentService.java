package com.mgads.appmoviles.classtrack.students;

import com.mgads.appmoviles.classtrack.courses.CourseEntity;
import com.mgads.appmoviles.classtrack.courses.CourseService;
import com.mgads.appmoviles.classtrack.exception.BadRequestException;
import com.mgads.appmoviles.classtrack.exception.ConflictException;
import com.mgads.appmoviles.classtrack.exception.NotFoundException;
import com.mgads.appmoviles.classtrack.exception.UnauthorizedException;
import com.mgads.appmoviles.classtrack.users.UserEntity;
import com.mgads.appmoviles.classtrack.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseStudentService {

    private final CourseStudentRepository courseStudentRepository;
    private final CourseService courseService;
    private final UserRepository userRepository;

    @Transactional
    public void linkStudent(Long courseId, Long teacherId, Long studentId) {
        CourseEntity course = courseService.findActive(courseId);

        if (!course.getTeacher().getId().equals(teacherId)) {
            throw new UnauthorizedException("You do not own this course");
        }

        UserEntity student = userRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Student not found"));

        if (student.getRole() != UserEntity.Role.STUDENT) {
            throw new BadRequestException("User is not a student");
        }

        if (courseStudentRepository.existsByCourseIdAndStudentId(courseId, studentId)) {
            throw new ConflictException("Student already linked to this course");
        }

        CourseStudentEntity link = new CourseStudentEntity();
        link.setCourse(course);
        link.setStudent(student);
        courseStudentRepository.save(link);
    }

    public List<StudentResponse> listStudents(Long courseId, Long teacherId) {
        CourseEntity course = courseService.findActive(courseId);

        if (!course.getTeacher().getId().equals(teacherId)) {
            throw new UnauthorizedException("You do not own this course");
        }

        return courseStudentRepository.findStudentsByCourseId(courseId).stream()
                .map(row -> new StudentResponse(
                        (Long) row[0],
                        (String) row[1],
                        (String) row[2]
                ))
                .toList();
    }

    @Transactional
    public void removeStudent(Long courseId, Long teacherId, Long studentId) {
        CourseEntity course = courseService.findActive(courseId);

        if (!course.getTeacher().getId().equals(teacherId)) {
            throw new UnauthorizedException("You do not own this course");
        }

        if (!courseStudentRepository.existsByCourseIdAndStudentId(courseId, studentId)) {
            throw new NotFoundException("Student not linked to this course");
        }

        courseStudentRepository.deleteByCourseIdAndStudentId(courseId, studentId);
    }
}
