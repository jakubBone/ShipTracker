package com.shiptracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiptracker.dto.LoginRequest;
import com.shiptracker.entity.User;
import com.shiptracker.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/*
 * @SpringBootTest is used instead of @WebMvcTest because @WebMvcTest only loads
 * the HTTP layer and does not fully initialize SecurityConfig.
 *
 * When @MockitoBean AuthenticationManager is registered, the sliced context does
 * not create the SecurityConfig.userDetailsService() bean (nothing needs it yet).
 * As a result, UserDetailsServiceAutoConfiguration creates its own
 * inMemoryUserDetailsManager, Spring Boot applies the default HTTP Basic security,
 * and the permitAll() rule on /api/auth/login never takes effect.
 *
 * @SpringBootTest loads the full application context (H2 from test/resources),
 * so SecurityConfig is fully initialized and permitAll() works as expected.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserRepository userRepository;

    private User buildUser(String username, String rawPassword) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole("ROLE_USER");
        return user;
    }

    // --- POST /api/auth/login ---

    @Test
    void login_valid() throws Exception {
        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(buildUser("admin", "password")));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged in successfully"));
    }

    @Test
    void login_invalid() throws Exception {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "wrong"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_blankFields() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // --- POST /api/auth/logout ---

    @Test
    @WithMockUser
    void logout() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }
}
