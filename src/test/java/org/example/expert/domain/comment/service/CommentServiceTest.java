package org.example.expert.domain.comment.service;

import org.assertj.core.groups.Tuple;
import org.example.expert.domain.comment.dto.request.CommentSaveRequest;
import org.example.expert.domain.comment.dto.response.CommentResponse;
import org.example.expert.domain.comment.dto.response.CommentSaveResponse;
import org.example.expert.domain.comment.entity.Comment;
import org.example.expert.domain.comment.repository.CommentRepository;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private TodoRepository todoRepository;
    @InjectMocks
    private CommentService commentService;

    @Test
    public void comment_등록_중_할일을_찾지_못해_에러가_발생한다() {
        // given
        long todoId = 1;
        CommentSaveRequest request = new CommentSaveRequest("contents");
        AuthUser authUser = new AuthUser(1L, "email", UserRole.USER);

        given(todoRepository.findById(anyLong())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.saveComment(authUser, todoId, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Todo not found");
    }

    @Test
    public void comment를_정상적으로_등록한다() {
        // given
        long todoId = 1;
        CommentSaveRequest request = new CommentSaveRequest("contents");
        AuthUser authUser = new AuthUser(1L, "email", UserRole.USER);
        User user = User.fromAuthUser(authUser);
        Todo todo = new Todo("title", "title", "contents", user);
        Comment comment = new Comment(request.getContents(), user, todo);

        given(todoRepository.findById(anyLong())).willReturn(Optional.of(todo));
        given(commentRepository.save(any())).willReturn(comment);

        // when
        CommentSaveResponse result = commentService.saveComment(authUser, todoId, request);

        // then
        assertNotNull(result);
    }

    @Test
    @DisplayName("댓글 목록을 조회한다.")
    void success_getComments() {
        // given
        User user1 = new User("test1@test.com", "12345", UserRole.USER);
        User user2 = new User("test2@test.com", "12345", UserRole.USER);

        Comment comment1 = new Comment("contents1", user1, null);
        Comment comment2 = new Comment("contents2", user1, null);
        Comment comment3 = new Comment("contents3", user2, null);

        given(commentRepository.findByTodoIdWithUser(anyLong()))
                .willReturn(List.of(
                        comment1,
                        comment2,
                        comment3
                ));

        long todoId = 1L;

        // when
        List<CommentResponse> commentResponses = commentService.getComments(todoId);

        // then
        assertThat(commentResponses)
                .extracting("contents", "user.email")
                .contains(
                        Tuple.tuple("contents1", "test1@test.com"),
                        Tuple.tuple("contents2", "test1@test.com"),
                        Tuple.tuple("contents3", "test2@test.com")
                );
    }
}
