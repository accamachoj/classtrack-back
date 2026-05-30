package com.mgads.appmoviles.classtrack.attendance;

import com.mgads.appmoviles.classtrack.exception.BadRequestException;
import com.mgads.appmoviles.classtrack.exception.ConflictException;
import com.mgads.appmoviles.classtrack.exception.UnauthorizedException;
import com.mgads.appmoviles.classtrack.students.CourseStudentRepository;
import com.mgads.appmoviles.classtrack.users.UserEntity;
import com.mgads.appmoviles.classtrack.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRecordRepository recordRepository;
    private final AttendanceSessionService sessionService;
    private final CourseStudentRepository courseStudentRepository;
    private final UserRepository userRepository;

    @Transactional
    public CheckinResponse checkin(Long studentId, CheckinRequest request) {
        AttendanceSessionEntity session = sessionService.findById(request.getSessionId());

        if (session.getStatus() != AttendanceSessionEntity.Status.ACTIVE) {
            throw new BadRequestException("Session is not active");
        }

        if (LocalDateTime.now().isAfter(session.getExpiresAt())) {
            throw new BadRequestException("Session QR has expired");
        }

        Long courseId = session.getCourse().getId();
        if (!courseStudentRepository.existsByCourseIdAndStudentId(courseId, studentId)) {
            throw new UnauthorizedException("You are not enrolled in this course");
        }

        if (recordRepository.existsBySessionIdAndStudentId(request.getSessionId(), studentId)) {
            throw new ConflictException("Attendance already registered for this session");
        }

        UserEntity student = userRepository.findById(studentId)
                .orElseThrow(() -> new BadRequestException("Student not found"));

        LocalDateTime now = LocalDateTime.now();

        AttendanceRecordEntity record = new AttendanceRecordEntity();
        record.setSession(session);
        record.setStudent(student);
        record.setLatitude(request.getLatitude());
        record.setLongitude(request.getLongitude());
        record.setRegisteredAt(now);

        AttendanceRecordEntity saved = recordRepository.save(record);

        return new CheckinResponse(saved.getId(), saved.getRegisteredAt());
    }

    public List<AttendanceRecordResponse> getSessionRecords(Long sessionId, Long teacherId) {
        AttendanceSessionEntity session = sessionService.findById(sessionId);

        if (!session.getCourse().getTeacher().getId().equals(teacherId)) {
            throw new UnauthorizedException("You do not own this session");
        }

        return recordRepository.findRecordsBySessionId(sessionId).stream()
                .map(row -> new AttendanceRecordResponse(
                        (Long) row[0],
                        (String) row[1],
                        (LocalDateTime) row[2]
                ))
                .toList();
    }

    public List<AttendanceHistoryResponse> getStudentHistory(Long studentId) {
        return recordRepository.findByStudentId(studentId).stream()
                .map(record -> new AttendanceHistoryResponse(
                        record.getSession().getId(),
                        record.getSession().getCourse().getName(),
                        record.getRegisteredAt(),
                        record.getLatitude(),
                        record.getLongitude()
                ))
                .toList();
    }
}
