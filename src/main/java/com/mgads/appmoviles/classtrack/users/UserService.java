package com.mgads.appmoviles.classtrack.users;

import com.mgads.appmoviles.classtrack.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getProfile(Long userId) {
        UserEntity user = findById(userId);
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole().name());
    }

    public DigitalIdResponse getDigitalId(Long userId) {
        UserEntity user = findById(userId);
        String qrContent = "CT_USER_" + user.getId();
        return new DigitalIdResponse(user.getId(), user.getFullName(), user.getStudentCode(), qrContent);
    }

    public Long resolveUserId(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"))
                .getId();
    }

    private UserEntity findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
