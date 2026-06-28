package com.fitness.userservice;

import com.fitness.userservice.dto.RegisterRequest;
import com.fitness.userservice.dto.UserResponse;
import com.fitness.userservice.repository.UserRepository;
import com.fitness.userservice.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("UserService Integration Tests")
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("register() + getUserProfile() - full round-trip persists and retrieves user")
    void register_thenGetProfile_roundTrip() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("integration@test.com");
        request.setPassword("secure-pass");
        request.setKeycloakId("kc-integration-001");
        request.setFirstName("Alice");
        request.setLastName("Smith");

        UserResponse registered = userService.register(request);

        assertThat(registered.getId()).isNotNull();
        assertThat(registered.getEmail()).isEqualTo("integration@test.com");
        assertThat(registered.getKeycloakId()).isEqualTo("kc-integration-001");

        UserResponse fetched = userService.getUserProfile(registered.getId());
        assertThat(fetched.getFirstName()).isEqualTo("Alice");
    }

    @Test
    @DisplayName("register() twice with same email returns existing user without duplicate")
    void register_duplicateEmail_returnsExistingUserNoDuplicate() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("dup@test.com");
        request.setPassword("pass");
        request.setKeycloakId("kc-dup-001");
        request.setFirstName("Bob");
        request.setLastName("Jones");

        userService.register(request);
        userService.register(request); // second call

        long count = userRepository.count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("existByUserId() - returns true for registered keycloakId")
    void existByUserId_afterRegister_returnsTrue() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("validate@test.com");
        request.setPassword("pass");
        request.setKeycloakId("kc-validate-001");
        request.setFirstName("Carol");
        request.setLastName("White");

        userService.register(request);

        assertThat(userService.existByUserId("kc-validate-001")).isTrue();
        assertThat(userService.existByUserId("kc-unknown")).isFalse();
    }

    @Test
    @DisplayName("getUserProfile() - throws RuntimeException for non-existent userId")
    void getUserProfile_unknownId_throwsException() {
        assertThatThrownBy(() -> userService.getUserProfile("non-existent-id"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User Not Found");
    }
}
