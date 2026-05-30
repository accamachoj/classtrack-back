package com.mgads.appmoviles.classtrack.attendance;

import com.mgads.appmoviles.classtrack.courses.CourseEntity;
import com.mgads.appmoviles.classtrack.courses.CourseService;
import com.mgads.appmoviles.classtrack.exception.BadRequestException;
import com.mgads.appmoviles.classtrack.exception.NotFoundException;
import com.mgads.appmoviles.classtrack.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceSessionService {

    @Value("${app.session.duration-minutes:15}")
    private long sessionDurationMinutes;

    private final AttendanceSessionRepository sessionRepository;
    private final CourseService courseService;

    @Transactional
    public SessionResponse createSession(Long teacherId, CreateSessionRequest request) {
        CourseEntity course = courseService.findActive(request.getCourseId());

        if (!course.getTeacher().getId().equals(teacherId)) {
            throw new UnauthorizedException("You do not own this course");
        }

        LocalDateTime now = LocalDateTime.now();

        AttendanceSessionEntity session = new AttendanceSessionEntity();
        session.setCourse(course);
        session.setQrToken("ATT_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        session.setStatus(AttendanceSessionEntity.Status.ACTIVE);
        session.setStartedAt(now);
        session.setExpiresAt(now.plusMinutes(sessionDurationMinutes));

        AttendanceSessionEntity saved = sessionRepository.save(session);

        return new SessionResponse(
                saved.getId(),
                saved.getCourse().getId(),
                saved.getQrToken(),
                saved.getExpiresAt()
        );
    }

    public SessionDetailResponse getSession(Long sessionId, Long teacherId) {
        AttendanceSessionEntity session = findById(sessionId);

        if (!session.getCourse().getTeacher().getId().equals(teacherId)) {
            throw new UnauthorizedException("You do not own this session");
        }

        return new SessionDetailResponse(
                session.getId(),
                session.getCourse().getId(),
                session.getStatus().name()
        );
    }

    @Transactional
    public void closeSession(Long sessionId, Long teacherId) {
        AttendanceSessionEntity session = findById(sessionId);

        if (!session.getCourse().getTeacher().getId().equals(teacherId)) {
            throw new UnauthorizedException("You do not own this session");
        }

        if (session.getStatus() == AttendanceSessionEntity.Status.CLOSED) {
            throw new BadRequestException("Session is already closed");
        }

        session.setStatus(AttendanceSessionEntity.Status.CLOSED);
        session.setClosedAt(LocalDateTime.now());
        sessionRepository.save(session);
    }

    public AttendanceSessionEntity findById(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Session not found"));
    }
}
