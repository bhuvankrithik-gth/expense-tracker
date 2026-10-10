package com.ledgertracker.service;

import com.ledgertracker.dto.RegisterRequest;
import com.ledgertracker.dto.UserDto;
import com.ledgertracker.exception.BadRequestException;
import com.ledgertracker.exception.ResourceNotFoundException;
import com.ledgertracker.model.User;
import com.ledgertracker.repository.CategoryRepository;
import com.ledgertracker.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       CategoryRepository categoryRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserDto register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BadRequestException("An account with this email address already exists. Please sign in instead.");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setAuthProvider("LOCAL");
        user.setGoogleSub(null);
        user.setDisplayName(request.getDisplayName().trim());
        user.setAvatarUrl(null);
        user.setEmailVerified(false);

        User savedUser = userRepository.create(user);

        // Seed initial default categories within the same transaction
        categoryRepository.seedDefaultCategoriesForUser(savedUser.getId());

        return toDto(savedUser);
    }

    public UserDto getUserDto(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return toDto(user);
    }

    public UserDto toDto(User user) {
        boolean hasPassword = user.getPasswordHash() != null && !user.getPasswordHash().isBlank();
        boolean hasGoogle = user.getGoogleSub() != null && !user.getGoogleSub().isBlank();
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getAuthProvider(),
                user.isEmailVerified(),
                hasPassword,
                hasGoogle,
                user.getCreatedAt()
        );
    }
}
