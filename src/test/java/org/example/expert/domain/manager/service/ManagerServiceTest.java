package org.example.expert.domain.manager.service;

import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.manager.dto.request.ManagerSaveRequest;
import org.example.expert.domain.manager.dto.response.ManagerResponse;
import org.example.expert.domain.manager.dto.response.ManagerSaveResponse;
import org.example.expert.domain.manager.entity.Manager;
import org.example.expert.domain.manager.repository.ManagerRepository;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.example.expert.utils.TestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ManagerServiceTest {

    @Mock
    private ManagerRepository managerRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TodoRepository todoRepository;
    @InjectMocks
    private ManagerService managerService;

    @Test
    public void manager_목록_조회_시_Todo가_없다면_InvalidRequestException_에러를_던진다() {
        // given
        long todoId = 1L;
        given(todoRepository.findById(todoId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> managerService.getManagers(todoId))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Todo not found");
    }

    @Test
    void todo의_user가_null인_경우_예외가_발생한다() {
        // given
        AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
        long todoId = 1L;
        long managerUserId = 2L;

        Todo todo = new Todo();
        ReflectionTestUtils.setField(todo, "user", null);

        ManagerSaveRequest managerSaveRequest = new ManagerSaveRequest(managerUserId);

        given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));

        // when & then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
                managerService.saveManager(authUser, todoId, managerSaveRequest)
        );

        assertEquals("일정을 생성한 유저만 담당자를 지정할 수 있습니다.", exception.getMessage());
    }

    @Test // 테스트코드 샘플
    public void manager_목록_조회에_성공한다() {
        // given
        long todoId = 1L;
        User user = new User("user1@example.com", "password", UserRole.USER);
        Todo todo = new Todo("Title", "Contents", "Sunny", user);
        ReflectionTestUtils.setField(todo, "id", todoId);

        Manager mockManager = new Manager(todo.getUser(), todo);
        List<Manager> managerList = List.of(mockManager);

        given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));
        given(managerRepository.findByTodoIdWithUser(todoId)).willReturn(managerList);

        // when
        List<ManagerResponse> managerResponses = managerService.getManagers(todoId);

        // then
        assertEquals(1, managerResponses.size());
        assertEquals(mockManager.getId(), managerResponses.get(0).getId());
        assertEquals(mockManager.getUser().getEmail(), managerResponses.get(0).getUser().getEmail());
    }

    @Test
        // 테스트코드 샘플
    void todo가_정상적으로_등록된다() {
        // given
        AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
        User user = User.fromAuthUser(authUser);  // 일정을 만든 유저

        long todoId = 1L;
        Todo todo = new Todo("Test Title", "Test Contents", "Sunny", user);

        long managerUserId = 2L;
        User managerUser = new User("b@b.com", "password", UserRole.USER);  // 매니저로 등록할 유저
        ReflectionTestUtils.setField(managerUser, "id", managerUserId);

        ManagerSaveRequest managerSaveRequest = new ManagerSaveRequest(managerUserId); // request dto 생성

        given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));
        given(userRepository.findById(managerUserId)).willReturn(Optional.of(managerUser));
        given(managerRepository.save(any(Manager.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        ManagerSaveResponse response = managerService.saveManager(authUser, todoId, managerSaveRequest);

        // then
        assertNotNull(response);
        assertEquals(managerUser.getId(), response.getUser().getId());
        assertEquals(managerUser.getEmail(), response.getUser().getEmail());
    }

    @Test // 테스트코드 샘플
    @DisplayName("일정 작성자는 본인을 담당자로 등록할 수 없습니다.")
    void saveManagerFails_whenWriterIsAssignedAsAssignee() {
        // given
        AuthUser authUser = new AuthUser(1L, "test@test.com", UserRole.USER);
        User user = TestUtils.createEntity(User.class, Map.of(
                "id", 1L,
                "email", "test@test.com",
                "userRole", UserRole.USER
        ));

        long todoId = 1L;
        Todo todo = TestUtils.createEntity(Todo.class, Map.of(
                "id", todoId,
                "title", "Test Title",
                "contents", "Test Contents",
                "weather", "Sunny",
                "user", user
        ));

        long managerUserId = 1L;
        User managerUser = TestUtils.createEntity(User.class, Map.of(
                "id", managerUserId,
                "email", "b@b.com",
                "password", "password",
                "userRole", UserRole.USER
        ));

        ManagerSaveRequest managerSaveRequest = new ManagerSaveRequest(managerUserId); // request dto 생성

        given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));
        given(userRepository.findById(managerUserId)).willReturn(Optional.of(managerUser));

        // when & then
        assertThatThrownBy(() -> managerService.saveManager(authUser, todoId, managerSaveRequest))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("일정 작성자는 본인을 담당자로 등록할 수 없습니다.");
    }

    @Test
    @DisplayName("일정을 등록한 담당자는 일정을 삭제할 수 있다.")
    void deleteManager() {
        // given
        long userId = 1L;
        User user = TestUtils.createEntity(User.class, Map.of(
                "id", userId,
                "email", "test@test.com",
                "userRole", UserRole.USER
        ));

        long todoId = 1L;
        Todo todo = TestUtils.createEntity(Todo.class, Map.of(
                "id", todoId,
                "title", "Test Title",
                "contents", "Test Contents",
                "weather", "Sunny",
                "user", user
        ));

        long managerId = 1L;
        Manager manager = TestUtils.createEntity(Manager.class, Map.of(
                "id", managerId,
                "user", user,
                "todo", todo
        ));

        given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
        given(todoRepository.findById(anyLong())).willReturn(Optional.of(todo));
        given(managerRepository.findById(anyLong())).willReturn(Optional.of(manager));

        // when
        managerService.deleteManager(userId, todoId, managerId);

        // then
        verify(managerRepository, times(1)).delete(manager);
    }

    @Test
    @DisplayName("해당 일정을 만든 유저가 유효하지 않습니다.")
    void deleteManagerFails_whenWriterIsInvalid() {
        // given
        long userId = 1L;
        User user = TestUtils.createEntity(User.class, Map.of(
                "id", userId,
                "email", "test@test.com",
                "userRole", UserRole.USER
        ));

        User writer = TestUtils.createEntity(User.class, Map.of(
                "id", 2L,
                "email", "writer@writer.com",
                "userRole", UserRole.USER
        ));

        long todoId = 1L;
        Todo todo = TestUtils.createEntity(Todo.class, Map.of(
                "id", todoId,
                "title", "Test Title",
                "contents", "Test Contents",
                "weather", "Sunny",
                "user", writer
        ));

        long managerId = 1L;

        given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
        given(todoRepository.findById(anyLong())).willReturn(Optional.of(todo));

        // when & then
        assertThatThrownBy(() -> managerService.deleteManager(userId, todoId, managerId))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("해당 일정을 만든 유저가 유효하지 않습니다.");
    }

    @Test
    @DisplayName("일정 등록인이 존재하지 않는다면 에러를 반환한다.")
    void deleteManagerFails_whenWriterIsNull() {
        // given
        long userId = 1L;
        User user = TestUtils.createEntity(User.class, Map.of(
                "id", userId,
                "email", "test@test.com",
                "userRole", UserRole.USER
        ));

        long todoId = 1L;
        Todo todo = TestUtils.createEntity(Todo.class, Map.of(
                "id", todoId,
                "title", "Test Title",
                "contents", "Test Contents",
                "weather", "Sunny"
        ));

        long managerId = 1L;

        given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
        given(todoRepository.findById(anyLong())).willReturn(Optional.of(todo));

        // when & then
        assertThatThrownBy(() -> managerService.deleteManager(userId, todoId, managerId))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("해당 일정을 만든 유저가 유효하지 않습니다.");
    }

    @Test
    @DisplayName("일정을 등록한 담당자는 일정을 삭제할 수 있다.")
    void deleteManagerFails_whenUserIsNotAssigneeOfTodo() {
        // given
        long userId = 1L;
        User user = TestUtils.createEntity(User.class, Map.of(
                "id", userId
        ));

        long todoId = 1L;
        Todo todo = TestUtils.createEntity(Todo.class, Map.of(
                "id", todoId,
                "user", user
        ));

        Todo anothorTodo = TestUtils.createEntity(Todo.class, Map.of(
                "id", 2L
        ));

        long managerId = 1L;
        Manager manager = TestUtils.createEntity(Manager.class, Map.of(
                "id", managerId,
                "user", user,
                "todo", anothorTodo
        ));

        given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
        given(todoRepository.findById(anyLong())).willReturn(Optional.of(todo));
        given(managerRepository.findById(anyLong())).willReturn(Optional.of(manager));

        // when & then
        assertThatThrownBy(() -> managerService.deleteManager(userId, todoId, managerId))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("해당 일정에 등록된 담당자가 아닙니다.");
    }
}
