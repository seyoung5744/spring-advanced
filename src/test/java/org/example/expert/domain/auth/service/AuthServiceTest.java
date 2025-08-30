package org.example.expert.domain.auth.service;

import org.example.expert.config.JwtUtil;
import org.example.expert.config.PasswordEncoder;
import org.example.expert.domain.auth.dto.request.SigninRequest;
import org.example.expert.domain.auth.dto.request.SignupRequest;
import org.example.expert.domain.auth.dto.response.SigninResponse;
import org.example.expert.domain.auth.dto.response.SignupResponse;
import org.example.expert.domain.auth.exception.AuthException;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtUtil jwtUtil;

    @InjectMocks
    AuthService authService;

    @Test
    @DisplayName("첫 가입자는 회원가입이 정상적으로 이뤄진다.")
    void signup() {
        // given
        Long userId = 1L;
        String email = "test@test.com";
        String encodedPassword = "testtesttest";
        String password = "test12345";
        UserRole userRole = UserRole.USER;

        User savedUser = new User(email, encodedPassword, userRole);
        ReflectionTestUtils.setField(savedUser, "id", userId);

        SignupRequest signupRequest = new SignupRequest(email, password, userRole.toString());
        String bearerToken = "bearerToken";

        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn(encodedPassword);
        given(jwtUtil.createToken(anyLong(), anyString(), any(UserRole.class))).willReturn(bearerToken);
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        // when
        SignupResponse signupResponse = authService.signup(signupRequest);

        // then
        assertThat(signupResponse.getBearerToken()).isEqualTo(bearerToken);

        // 실제 save()에 전달된 User 값 검증
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();

        assertThat(capturedUser.getEmail()).isEqualTo(email);
        assertThat(capturedUser.getPassword()).isEqualTo(encodedPassword);
        assertThat(capturedUser.getUserRole()).isEqualTo(userRole);
    }

    @Test
    @DisplayName("이미 가입된 이메일은 중복 가입될 수 없다.")
    void signUpAlreadyExistEmail() {
        // given
        String email = "test@test.com";
        String password = "test12345";
        UserRole userRole = UserRole.USER;

        SignupRequest signupRequest = new SignupRequest(email, password, userRole.toString());
        given(userRepository.existsByEmail(anyString())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.signup(signupRequest))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("이미 존재하는 이메일입니다.");
    }

    @Test
    @DisplayName("로그인에 성공하면 JWT을 응답한다.")
    void signIn() {
        // given
        Long userId = 1L;
        String email = "test@test.com";
        String encodedPassword = "testtesttest";
        String password = "test12345";
        UserRole userRole = UserRole.USER;

        User savedUser = new User(email, encodedPassword, userRole);
        ReflectionTestUtils.setField(savedUser, "id", userId);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(savedUser));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);

        String bearerToken = "bearerToken";
        given(jwtUtil.createToken(anyLong(), anyString(), any(UserRole.class))).willReturn(bearerToken);

        SigninRequest signinRequest = new SigninRequest(email, password);

        // when
        SigninResponse signinResponse = authService.signin(signinRequest);

        // then
        assertThat(signinResponse.getBearerToken()).isEqualTo(bearerToken);
        verify(userRepository, times(1)).findByEmail(email);
    }

    @Test
    @DisplayName("가입되지 않은 회원은 로그인이 불가능하다.")
    void signInFails_whenUserNotRegistered() {
        // given
        String email = "test@test.com";
        String password = "test12345";
        given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());

        SigninRequest signinRequest = new SigninRequest(email, password);

        // when & then
        assertThatThrownBy(() -> authService.signin(signinRequest))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("가입되지 않은 유저입니다.");
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 로그인이 불가능하다.")
    void signInFails_inValidPassword() {
        // given
        Long userId = 1L;
        String email = "test@test.com";
        String encodedPassword = "testtesttest";
        String password = "test12345";
        UserRole userRole = UserRole.USER;

        User savedUser = new User(email, encodedPassword, userRole);
        ReflectionTestUtils.setField(savedUser, "id", userId);

        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(savedUser));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

        SigninRequest signinRequest = new SigninRequest(email, password);

        // when & then
        assertThatThrownBy(() -> authService.signin(signinRequest))
                .isInstanceOf(AuthException.class)
                .hasMessage("잘못된 비밀번호입니다.");
    }
}