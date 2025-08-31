package org.example.expert.domain.user.service;

import org.example.expert.domain.user.dto.request.UserRoleChangeRequest;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserAdminService userAdminService;

    @Test
    @DisplayName("관리자는 유저 권한을 변경할 수 있다.")
    void changeUserRole() {
        // given
        long userId = 1L;
        UserRoleChangeRequest userRoleChangeRequest = new UserRoleChangeRequest(UserRole.ADMIN.toString());

        User user = new User();
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(user, "userRole", UserRole.USER);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        userAdminService.changeUserRole(userId, userRoleChangeRequest);

        // then
        assertThat(user.getUserRole()).isEqualTo(UserRole.ADMIN);
        verify(userRepository).findById(userId);
    }
}