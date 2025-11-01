package com.bank.auth.service;


import com.bank.auth.entity.User;
import com.bank.auth.repository.UserRepository;
import com.bank.common.dto.contracts.auth.UserDTO;
import com.bank.common.dto.contracts.auth.UserRegistrationRequest;
import com.bank.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserDTO registerUser(UserRegistrationRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = User.builder()
            .username(request.getUsername())
            .password(passwordEncoder.encode(request.getPassword()))
            .enabled(true)
            .roles(Set.of("USER"))
            .build();

        User saved = userRepository.save(user);
        return mapToDto(saved);
    }

    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    public UserDTO findByUsername(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("User not found"));
        return mapToDto(user);
    }

    public void changePassword(String username, String newPassword) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        User u = userRepository.save(user);
        if (u.getPassword().equals(user.getPassword())) {
            log.info("&#x2714; Password changed successfully for user {}", username);
        } else {
            log.info("&#x2716; Password change failed for user {}", username);
        }
    }

    private UserDTO mapToDto(User user) {
        return new UserDTO(
            user.getId(),
            user.getUsername(),
            user.getRoles(),
            user.isEnabled(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}
