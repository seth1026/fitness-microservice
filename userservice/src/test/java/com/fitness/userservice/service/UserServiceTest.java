package com.fitness.userservice.service;

import com.fitness.userservice.dto.RegisterRequest;
import com.fitness.userservice.dto.UserResponse;
import com.fitness.userservice.model.User;
import com.fitness.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository repository;

    @InjectMocks
    private UserService userService;

    private RegisterRequest registerRequest;
    private User existingUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setKeycloakId("kc-uuid-123");
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");

        existingUser = new User();
        existingUser.setId("user-id-001");
        existingUser.setEmail("test@example.com");
        existingUser.setPassword("password123");
        existingUser.setKeycloakId("kc-uuid-123");
        existingUser.setFirstName("John");
        existingUser.setLastName("Doe");
        existingUser.setCreatedAt(LocalDateTime.now());
        existingUser.setUpdatedAt(LocalDateTime.now());
    }

    // ─── register() ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("register() - creates a new user when email does not exist")
    void register_newUser_savesAndReturnsResponse() {
        when(repository.existsByEmail("test@example.com")).thenReturn(false);
        when(repository.save(any(User.class))).thenReturn(existingUser);

        UserResponse response = userService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getKeycloakId()).isEqualTo("kc-uuid-123");
        verify(repository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("register() - returns existing user when email already registered")
    void register_existingEmail_returnsExistingUser() {
        when(repository.existsByEmail("test@example.com")).thenReturn(true);
        when(repository.findByEmail("test@example.com")).thenReturn(existingUser);

        UserResponse response = userService.register(registerRequest);

        assertThat(response.getId()).isEqualTo("user-id-001");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        // Should NOT save again since keycloakId already matches
        verify(repository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("register() - updates keycloakId when existing user has no keycloakId")
    void register_existingUserMissingKeycloakId_updatesAndSaves() {
        existingUser.setKeycloakId(null);
        when(repository.existsByEmail("test@example.com")).thenReturn(true);
        when(repository.findByEmail("test@example.com")).thenReturn(existingUser);
        when(repository.save(existingUser)).thenReturn(existingUser);

        UserResponse response = userService.register(registerRequest);

        assertThat(response).isNotNull();
        verify(repository, times(1)).save(existingUser);
    }

    // ─── getUserProfile() ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getUserProfile() - returns UserResponse for valid userId")
    void getUserProfile_validId_returnsResponse() {
        when(repository.findById("user-id-001")).thenReturn(Optional.of(existingUser));

        UserResponse response = userService.getUserProfile("user-id-001");

        assertThat(response.getId()).isEqualTo("user-id-001");
        assertThat(response.getFirstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("getUserProfile() - throws RuntimeException for unknown userId")
    void getUserProfile_unknownId_throwsException() {
        when(repository.findById("unknown-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserProfile("unknown-id"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User Not Found");
    }

    // ─── existByUserId() ──────────────────────────────────────────────────────

    @Test
    @DisplayName("existByUserId() - returns true when keycloakId exists")
    void existByUserId_existingKeycloakId_returnsTrue() {
        when(repository.existsByKeycloakId("kc-uuid-123")).thenReturn(true);

        assertThat(userService.existByUserId("kc-uuid-123")).isTrue();
    }

    @Test
    @DisplayName("existByUserId() - returns false when keycloakId not found")
    void existByUserId_unknownKeycloakId_returnsFalse() {
        when(repository.existsByKeycloakId("kc-unknown")).thenReturn(false);

        assertThat(userService.existByUserId("kc-unknown")).isFalse();
    }
}
