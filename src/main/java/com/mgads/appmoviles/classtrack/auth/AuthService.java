package com.mgads.appmoviles.classtrack.auth;

import com.mgads.appmoviles.classtrack.exception.BadRequestException;
import com.mgads.appmoviles.classtrack.exception.ConflictException;
import com.mgads.appmoviles.classtrack.security.JwtService;
import com.mgads.appmoviles.classtrack.users.UserEntity;
import com.mgads.appmoviles.classtrack.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered");
        }

        UserEntity user = new UserEntity();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setActive(true);

        if (request.getRole() == UserEntity.Role.STUDENT) {
            String studentCode = request.getStudentCode() != null && !request.getStudentCode().isBlank()
                    ? request.getStudentCode()
                    : generateStudentCode();
            user.setStudentCode(studentCode);
        }

        UserEntity saved = userRepository.save(user);
        String token = jwtService.generateToken(saved.getId(), saved.getEmail(), saved.getRole().name());

        return new AuthResponse(saved.getId(), token, saved.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }

        if (!user.getActive()) {
            throw new BadRequestException("Account is inactive");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        return new AuthResponse(user.getId(), token, user.getRole().name());
    }

    private String generateStudentCode() {
        return "CT" + System.currentTimeMillis();
    }
}
