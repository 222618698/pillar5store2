package com.p5store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p5store.config.JwtService;
import com.p5store.dto.request.AuthDtos.*;
import com.p5store.exception.DuplicateResourceException;
import com.p5store.service.AuthService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AuthService authService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;

    private final AuthResponse mockAuthResponse = new AuthResponse(
            "mock.jwt.token",
            "123e4567-e89b-12d3-a456-426614174000",
            "test@p5store.com",
            "Jane Smith",
            "CUSTOMER"
    );

    // ── POST /auth/register ────────────────────────────────────
    @Nested
    @DisplayName("POST /auth/register")
    class Register {

        @Test
        @DisplayName("returns 201 with token on successful registration")
        void returns201_onSuccess() throws Exception {
            given(authService.register(any())).willReturn(mockAuthResponse);

            RegisterRequest request = new RegisterRequest(
                    "Jane", "Smith", "jane@p5store.com", "Password1!", "+27831234567");

            mockMvc.perform(post("/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").value("mock.jwt.token"))
                    .andExpect(jsonPath("$.email").value("test@p5store.com"))
                    .andExpect(jsonPath("$.role").value("CUSTOMER"));
        }

        @Test
        @DisplayName("returns 400 when email is invalid")
        void returns400_whenEmailInvalid() throws Exception {
            RegisterRequest request = new RegisterRequest(
                    "Jane", "Smith", "not-an-email", "Password1!", null);

            mockMvc.perform(post("/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when password is too short")
        void returns400_whenPasswordTooShort() throws Exception {
            RegisterRequest request = new RegisterRequest(
                    "Jane", "Smith", "jane@p5store.com", "short", null);

            mockMvc.perform(post("/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when first name is blank")
        void returns400_whenFirstNameBlank() throws Exception {
            RegisterRequest request = new RegisterRequest(
                    "", "Smith", "jane@p5store.com", "Password1!", null);

            mockMvc.perform(post("/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 409 when email is already registered")
        void returns409_whenEmailDuplicate() throws Exception {
            given(authService.register(any())).willThrow(
                    new DuplicateResourceException("Email already registered: jane@p5store.com"));

            RegisterRequest request = new RegisterRequest(
                    "Jane", "Smith", "jane@p5store.com", "Password1!", null);

            mockMvc.perform(post("/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Email already registered: jane@p5store.com"));
        }
    }

    // ── POST /auth/login ───────────────────────────────────────
    @Nested
    @DisplayName("POST /auth/login")
    class Login {

        @Test
        @DisplayName("returns 200 with token on valid credentials")
        void returns200_onValidCredentials() throws Exception {
            given(authService.login(any())).willReturn(mockAuthResponse);

            LoginRequest request = new LoginRequest("jane@p5store.com", "Password1!");

            mockMvc.perform(post("/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").exists())
                    .andExpect(jsonPath("$.fullName").value("Jane Smith"));
        }

        @Test
        @DisplayName("returns 400 when email is missing")
        void returns400_whenEmailMissing() throws Exception {
            LoginRequest request = new LoginRequest("", "Password1!");

            mockMvc.perform(post("/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}
