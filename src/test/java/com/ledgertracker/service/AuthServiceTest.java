package com.ledgertracker.service;

import com.ledgertracker.dto.RegisterRequest;
import com.ledgertracker.dto.UserDto;
import com.ledgertracker.exception.BadRequestException;
import com.ledgertracker.model.User;
import com.ledgertracker.repository.CategoryRepository;
import com.ledgertracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, categoryRepository, passwordEncoder);
    }

    @Test
    void register_ShouldCreateUserAndSeedCategories_WhenValidRequest() {
        RegisterRequest request = new RegisterRequest("Julian Gray", "julian.gray@ledgerly.internal", "SecurePassword123!");

        when(userRepository.existsByEmail("julian.gray@ledgerly.internal")).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPasswordHash");

        User savedUser = new User();
        savedUser.setId(42L);
        savedUser.setEmail("julian.gray@ledgerly.internal");
        savedUser.setDisplayName("Julian Gray");
        savedUser.setAuthProvider("LOCAL");
        savedUser.setEmailVerified(false);

        when(userRepository.create(any(User.class))).thenReturn(savedUser);

        UserDto result = authService.register(request);

        assertNotNull(result);
        assertEquals(42L, result.getId());
        assertEquals("julian.gray@ledgerly.internal", result.getEmail());
        assertEquals("Julian Gray", result.getDisplayName());

        verify(userRepository, times(1)).create(any(User.class));
        verify(categoryRepository, times(1)).seedDefaultCategoriesForUser(42L);
    }

    @Test
    void register_ShouldThrowBadRequest_WhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("Julian Gray", "existing@ledgerly.internal", "SecurePassword123!");

        when(userRepository.existsByEmail("existing@ledgerly.internal")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(request));
        verify(userRepository, never()).create(any(User.class));
    }
}
