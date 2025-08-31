package org.example.expert.domain.user.service;

import org.example.expert.config.PasswordEncoder;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.dto.request.UserChangePasswordRequest;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.repository.UserRepository;
import org.example.expert.utils.TestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    UserService userService;

    @Test
    @DisplayName("유저 ID로 단일 유저 정보를 조회할 수 있다.")
    void getUser() {
        // given
        long userId = 1L;
        User testUser = TestUtils.createEntity(User.class, Map.of(
                "id", userId,
                "email", "test@test.com"
        ));

        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

        // when
        UserResponse userResponse = userService.getUser(userId);

        // then
        assertThat(userResponse)
                .extracting("id", "email")
                .contains(userId, "test@test.com");
    }

    @Test
    @DisplayName("유저 ID에 해당하는 유저 데이터가 없을 시 에러를 반환한다.")
    void getUserFails_whenUserNotFound() {
        // given
        long userId = 1L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getUser(userId))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("User not found");
    }

    @Test
    @DisplayName("새 비밀번호가 기존 비밀번호와 다르다면 비밀번호 변경이 가능하다.")
    void changePassword() {
        // given
        long userId = 1L;
        String oldPassword = "test12345";
        String newPassword = "new12345";
        User testUser = TestUtils.createEntity(User.class, Map.of(
                "id", userId,
                "password", oldPassword
        ));
        UserChangePasswordRequest userChangePasswordRequest = new UserChangePasswordRequest(
                oldPassword,
                newPassword
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches(userChangePasswordRequest.getNewPassword(), oldPassword)).willReturn(false);
        given(passwordEncoder.matches(userChangePasswordRequest.getOldPassword(), oldPassword)).willReturn(true);
        given(passwordEncoder.encode(userChangePasswordRequest.getNewPassword())).willReturn(newPassword);
        // when
        userService.changePassword(userId, userChangePasswordRequest);

        // then
        assertThat(testUser.getPassword()).isEqualTo(newPassword);
    }

    @Test
    @DisplayName("회원이 존재하지 않는다면 비밀번호 변경이 불가능하다.")
    void changePasswordFails_whenUserNotFound() {
        // given
        long userId = 1L;
        UserChangePasswordRequest userChangePasswordRequest = new UserChangePasswordRequest(
                "test12345",
                "new12345"
        );

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.changePassword(userId, userChangePasswordRequest))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("User not found");
    }

    @Test
    @DisplayName("새 비밀번호는 기존 비밀번호와 같을 수 없다.")
    void changePasswordFails_whenNewAndOldPasswordIsSame() {
        // given
        long userId = 1L;
        String oldPassword = "test12345";
        String newPassword = "test12345";
        User testUser = TestUtils.createEntity(User.class, Map.of(
                "id", userId,
                "password", oldPassword
        ));
        UserChangePasswordRequest userChangePasswordRequest = new UserChangePasswordRequest(
                oldPassword,
                newPassword
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches(userChangePasswordRequest.getNewPassword(), oldPassword)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.changePassword(userId, userChangePasswordRequest))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("새 비밀번호는 기존 비밀번호와 같을 수 없습니다.");
    }

    @Test
    @DisplayName("OldPassword 가 기존 비밀번호와 일치하지 않으면 비밀번호 변경에 실패한다.")
    void changePasswordFails_whenOldPasswordNotMatch() {
        // given
        long userId = 1L;
        String password = "good12345";
        User testUser = TestUtils.createEntity(User.class, Map.of(
                "id", userId,
                "password", password
        ));
        String oldPassword = "test12345";
        UserChangePasswordRequest userChangePasswordRequest = new UserChangePasswordRequest(
                oldPassword,
                "new12345"
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches(userChangePasswordRequest.getNewPassword(), password)).willReturn(false);
        given(passwordEncoder.matches(userChangePasswordRequest.getOldPassword(), testUser.getPassword())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> userService.changePassword(userId, userChangePasswordRequest))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("잘못된 비밀번호입니다.");
    }

}