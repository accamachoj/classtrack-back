package com.mgads.appmoviles.classtrack.reports;

import com.mgads.appmoviles.classtrack.attendance.AttendanceRecordRepository;
import com.mgads.appmoviles.classtrack.attendance.AttendanceSessionEntity;
import com.mgads.appmoviles.classtrack.attendance.AttendanceSessionRepository;
import com.mgads.appmoviles.classtrack.attendance.AttendanceSessionService;
import com.mgads.appmoviles.classtrack.courses.CourseEntity;
import com.mgads.appmoviles.classtrack.courses.CourseRepository;
import com.mgads.appmoviles.classtrack.courses.CourseService;
import com.mgads.appmoviles.classtrack.exception.NotFoundException;
import com.mgads.appmoviles.classtrack.exception.UnauthorizedException;
import com.mgads.appmoviles.classtrack.students.CourseStudentRepository;
import com.mgads.appmoviles.classtrack.users.UserEntity;
import com.mgads.appmoviles.classtrack.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final AttendanceSessionService sessionService;
    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final CourseService courseService;
    private final CourseRepository courseRepository;
    private final CourseStudentRepository courseStudentRepository;
    private final UserRepository userRepository;

    public SessionReportResponse getSessionReport(Long sessionId, Long teacherId) {
        AttendanceSessionEntity session = sessionService.findById(sessionId);

        if (!session.getCourse().getTeacher().getId().equals(teacherId)) {
            throw new UnauthorizedException("You do not own this session");
        }

        long totalStudents = courseRepository.countStudentsByCourseId(session.getCourse().getId());
        long attendees = recordRepository.countBySessionId(sessionId);
        long absent = totalStudents - attendees;

        return new SessionReportResponse(sessionId, totalStudents, attendees, absent);
    }

    public CourseReportResponse getCourseReport(Long courseId, Long teacherId) {
        CourseEntity course = courseService.findActive(courseId);

        if (!course.getTeacher().getId().equals(teacherId)) {
            throw new UnauthorizedException("You do not own this course");
        }

        long totalStudents = courseRepository.countStudentsByCourseId(courseId);
        List<AttendanceSessionEntity> sessions = sessionRepository.findByCourseId(courseId);

        double attendanceRate = 0;
        if (!sessions.isEmpty() && totalStudents > 0) {
            long totalAttendances = sessions.stream()
                    .mapToLong(s -> recordRepository.countBySessionId(s.getId()))
                    .sum();
            long totalPossible = sessions.size() * totalStudents;
            attendanceRate = Math.round((double) totalAttendances / totalPossible * 100.0);
        }

        return new CourseReportResponse(courseId, course.getName(), totalStudents, attendanceRate);
    }

    public StudentReportResponse getStudentReport(Long studentId) {
        UserEntity student = userRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Student not found"));

        List<AttendanceSessionEntity> sessions = sessionRepository.findSessionsByStudentId(studentId);

        long sessionsAttended = sessions.stream()
                .filter(s -> recordRepository.existsBySessionIdAndStudentId(s.getId(), studentId))
                .count();

        double attendancePercentage = sessions.isEmpty()
                ? 0
                : Math.round((double) sessionsAttended / sessions.size() * 100.0);

        return new StudentReportResponse(studentId, student.getFullName(), attendancePercentage, sessionsAttended);
    }
}
