package com.mgads.appmoviles.classtrack.courses;

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
public class CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @Transactional
    public CourseResponse create(Long teacherId, CreateCourseRequest request) {
        UserEntity teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new NotFoundException("Teacher not found"));

        CourseEntity course = new CourseEntity();
        course.setTeacher(teacher);
        course.setName(request.getName());
        course.setDescription(request.getDescription());
        course.setActive(true);

        CourseEntity saved = courseRepository.save(course);
        return toResponse(saved);
    }

    public List<CourseListResponse> listForUser(Long userId, String role) {
        List<CourseEntity> courses = role.equals("TEACHER")
                ? courseRepository.findByTeacherIdAndActiveTrue(userId)
                : courseRepository.findByStudentId(userId);

        return courses.stream()
                .map(c -> new CourseListResponse(
                        c.getId(),
                        c.getName(),
                        courseRepository.countStudentsByCourseId(c.getId())
                ))
                .toList();
    }

    public CourseResponse getById(Long courseId) {
        CourseEntity course = findActive(courseId);
        return toResponse(course);
    }

    @Transactional
    public void delete(Long courseId, Long teacherId) {
        CourseEntity course = findActive(courseId);

        if (!course.getTeacher().getId().equals(teacherId)) {
            throw new UnauthorizedException("You do not own this course");
        }

        course.setActive(false);
        courseRepository.save(course);
    }

    public CourseEntity findActive(Long courseId) {
        CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found"));
        if (!course.getActive()) {
            throw new NotFoundException("Course not found");
        }
        return course;
    }

    private CourseResponse toResponse(CourseEntity course) {
        long studentCount = courseRepository.countStudentsByCourseId(course.getId());
        return new CourseResponse(course.getId(), course.getName(), course.getDescription(), studentCount);
    }
}
