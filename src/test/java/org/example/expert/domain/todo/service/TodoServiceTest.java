package org.example.expert.domain.todo.service;

import org.example.expert.client.WeatherClient;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.todo.dto.request.TodoSaveRequest;
import org.example.expert.domain.todo.dto.response.TodoResponse;
import org.example.expert.domain.todo.dto.response.TodoSaveResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    @Mock
    TodoRepository todoRepository;

    @Mock
    WeatherClient weatherClient;

    @InjectMocks
    TodoService todoService;

    @Test
    @DisplayName("Todo 를 성공적으로 저장한다.")
    void success_saveTodo() {
        // given
        AuthUser authUser = new AuthUser(1L, "email", UserRole.USER);
        TodoSaveRequest todoSaveRequest = new TodoSaveRequest("title", "contents");
        User user = User.fromAuthUser(authUser);
        Todo todo = new Todo("title", "title", "contents", user);
        ReflectionTestUtils.setField(todo, "id", 1L);

        given(weatherClient.getTodayWeather()).willReturn("weather");
        given(todoRepository.save(any(Todo.class))).willReturn(todo);

        // when
        TodoSaveResponse todoSaveResponse = todoService.saveTodo(authUser, todoSaveRequest);

        // then
        assertThat(todoSaveResponse)
                .extracting("id", "title", "contents", "weather")
                .contains(1L, "title", "title", "weather");
    }

    @Test
    @DisplayName("Todo 리스트 paging 하여 를 조회한다.")
    void success_getTodos() {
        // given
        User user = new User("email", "password", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 1L);

        Todo todo1 = new Todo("title1", "title1", "contents1", user);
        ReflectionTestUtils.setField(todo1, "id", 1L);
        ReflectionTestUtils.setField(todo1, "modifiedAt", LocalDateTime.of(2025, 8, 31, 4, 45, 10, 1));

        Todo todo2 = new Todo("title2", "title2", "contents2", user);
        ReflectionTestUtils.setField(todo2, "id", 2L);
        ReflectionTestUtils.setField(todo1, "modifiedAt", LocalDateTime.of(2025, 8, 31, 4, 45, 10, 2));

        Todo todo3 = new Todo("title3", "title3", "contents3", user);
        ReflectionTestUtils.setField(todo3, "id", 3L);
        ReflectionTestUtils.setField(todo1, "modifiedAt", LocalDateTime.of(2025, 8, 31, 4, 45, 10, 3));

        Todo todo4 = new Todo("title4", "title4", "contents4", user);
        ReflectionTestUtils.setField(todo4, "id", 4L);
        ReflectionTestUtils.setField(todo1, "modifiedAt", LocalDateTime.of(2025, 8, 31, 4, 45, 10, 4));


        given(todoRepository.findAllByOrderByModifiedAtDesc(any(Pageable.class))).willReturn(
                new PageImpl<>(List.of(todo1, todo2, todo3), PageRequest.of(0, 3), 4)
        );

        // when
        Page<TodoResponse> todoResponses = todoService.getTodos(1, 3);

        // then
        assertThat(todoResponses.getContent()).hasSize(3);
    }

    @Test
    @DisplayName("Todo ID 로 단일 todo 를 조회할 수 있다.")
    void success_getTodo() {
        // given
        User user = new User("email", "password", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 1L);

        Todo todo1 = new Todo("title1", "title1", "contents1", user);
        ReflectionTestUtils.setField(todo1, "id", 1L);

        Todo todo2 = new Todo("title2", "title2", "contents2", user);
        ReflectionTestUtils.setField(todo2, "id", 2L);

        long todoId = todo1.getId();
        given(todoRepository.findByIdWithUser(todoId)).willReturn(Optional.of(todo1));

        // when
        TodoResponse todoResponse = todoService.getTodo(todoId);

        // then
        assertThat(todoResponse)
                .extracting("id", "title", "contents", "user.email")
                .contains(1L, "title1", "title1", "email");
    }

    @Test
    @DisplayName("단일 Todo를 조회할 때 Todo 가 존재하지 않는다면 에러를 반환한다.")
    void getTodoFails_whenTodoIsEmpty() {
        // given
        long todoId = 1L;
        given(todoRepository.findByIdWithUser(todoId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> todoService.getTodo(todoId))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Todo not found");
    }
}