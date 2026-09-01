package com.abhiai.abhiai_backend.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.abhiai.abhiai_backend.news.exception.InvalidNewsQueryException;
import com.abhiai.abhiai_backend.news.exception.NewsArticleNotFoundException;
import com.abhiai.abhiai_backend.news.exception.NewsProviderException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidMemoryException.class)
    public ResponseEntity<ApiError> handleInvalidMemory(InvalidMemoryException exception, WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(InvalidNewsQueryException.class)
    public ResponseEntity<ApiError> handleInvalidNewsQuery(InvalidNewsQueryException exception, WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(NewsArticleNotFoundException.class)
    public ResponseEntity<ApiError> handleNewsArticleNotFound(NewsArticleNotFoundException exception, WebRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(NewsProviderException.class)
    public ResponseEntity<ApiError> handleNewsProvider(NewsProviderException exception, WebRequest request) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(InvalidContentReportException.class)
    public ResponseEntity<ApiError> handleInvalidContentReport(InvalidContentReportException exception, WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(DuplicateContentReportException.class)
    public ResponseEntity<ApiError> handleDuplicateContentReport(DuplicateContentReportException exception, WebRequest request) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(ArticleNotFoundException.class)
    public ResponseEntity<ApiError> handleArticleNotFound(ArticleNotFoundException exception, WebRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(InvalidArticleException.class)
    public ResponseEntity<ApiError> handleInvalidArticle(InvalidArticleException exception, WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(DuplicateArticleLikeException.class)
    public ResponseEntity<ApiError> handleDuplicateArticleLike(DuplicateArticleLikeException exception, WebRequest request) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ApiError> handleEmailAlreadyRegistered(
            EmailAlreadyRegisteredException exception,
            WebRequest request) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(
            InvalidCredentialsException exception,
            WebRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(ConversationNotFoundException.class)
    public ResponseEntity<ApiError> handleConversationNotFound(
            ConversationNotFoundException exception,
            WebRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(DirectConversationNotFoundException.class)
    public ResponseEntity<ApiError> handleDirectConversationNotFound(
            DirectConversationNotFoundException exception,
            WebRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(DirectMessageNotFoundException.class)
    public ResponseEntity<ApiError> handleDirectMessageNotFound(
            DirectMessageNotFoundException exception,
            WebRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(InvalidDirectMessageException.class)
    public ResponseEntity<ApiError> handleInvalidDirectMessage(
            InvalidDirectMessageException exception,
            WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler({
            GroupConversationNotFoundException.class,
            GroupInvitationNotFoundException.class,
            GroupMessageNotFoundException.class
    })
    public ResponseEntity<ApiError> handleGroupResourceNotFound(
            RuntimeException exception,
            WebRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(InvalidGroupActionException.class)
    public ResponseEntity<ApiError> handleInvalidGroupAction(
            InvalidGroupActionException exception,
            WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler({CommunityNotFoundException.class, CommunityMembershipNotFoundException.class})
    public ResponseEntity<ApiError> handleCommunityResourceNotFound(
            RuntimeException exception,
            WebRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler({CommunitySlugAlreadyExistsException.class, DuplicateCommunityMembershipException.class})
    public ResponseEntity<ApiError> handleCommunityConflict(
            RuntimeException exception,
            WebRequest request) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(InvalidCommunityException.class)
    public ResponseEntity<ApiError> handleInvalidCommunity(
            InvalidCommunityException exception,
            WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFound(
            UserNotFoundException exception,
            WebRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(UsernameAlreadyTakenException.class)
    public ResponseEntity<ApiError> handleUsernameAlreadyTaken(
            UsernameAlreadyTakenException exception,
            WebRequest request) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(InvalidUsernameException.class)
    public ResponseEntity<ApiError> handleInvalidUsername(
            InvalidUsernameException exception,
            WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(InvalidFollowException.class)
    public ResponseEntity<ApiError> handleInvalidFollow(
            InvalidFollowException exception,
            WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(DuplicateFollowException.class)
    public ResponseEntity<ApiError> handleDuplicateFollow(
            DuplicateFollowException exception,
            WebRequest request) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(FollowRelationshipNotFoundException.class)
    public ResponseEntity<ApiError> handleFollowRelationshipNotFound(
            FollowRelationshipNotFoundException exception,
            WebRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<ApiError> handlePostNotFound(
            PostNotFoundException exception,
            WebRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(UnauthorizedActionException.class)
    public ResponseEntity<ApiError> handleUnauthorizedAction(
            UnauthorizedActionException exception,
            WebRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(InvalidPostException.class)
    public ResponseEntity<ApiError> handleInvalidPost(
            InvalidPostException exception,
            WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(PollNotFoundException.class)
    public ResponseEntity<ApiError> handlePollNotFound(PollNotFoundException exception, WebRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(InvalidPollException.class)
    public ResponseEntity<ApiError> handleInvalidPoll(InvalidPollException exception, WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(DuplicatePollVoteException.class)
    public ResponseEntity<ApiError> handleDuplicatePollVote(DuplicatePollVoteException exception, WebRequest request) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(InvalidMediaException.class)
    public ResponseEntity<ApiError> handleInvalidMedia(InvalidMediaException exception, WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(MediaStorageUnavailableException.class)
    public ResponseEntity<ApiError> handleMediaStorageUnavailable(
            MediaStorageUnavailableException exception,
            WebRequest request) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(MediaNotFoundException.class)
    public ResponseEntity<ApiError> handleMediaNotFound(MediaNotFoundException exception, WebRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(DuplicatePostLikeException.class)
    public ResponseEntity<ApiError> handleDuplicatePostLike(
            DuplicatePostLikeException exception,
            WebRequest request) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(PostLikeNotFoundException.class)
    public ResponseEntity<ApiError> handlePostLikeNotFound(
            PostLikeNotFoundException exception,
            WebRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(PostReplyNotFoundException.class)
    public ResponseEntity<ApiError> handlePostReplyNotFound(
            PostReplyNotFoundException exception,
            WebRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(InvalidReplyException.class)
    public ResponseEntity<ApiError> handleInvalidReply(
            InvalidReplyException exception,
            WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(DuplicatePostRepostException.class)
    public ResponseEntity<ApiError> handleDuplicatePostRepost(
            DuplicatePostRepostException exception,
            WebRequest request) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(PostRepostNotFoundException.class)
    public ResponseEntity<ApiError> handlePostRepostNotFound(
            PostRepostNotFoundException exception,
            WebRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(DuplicatePostBookmarkException.class)
    public ResponseEntity<ApiError> handleDuplicatePostBookmark(
            DuplicatePostBookmarkException exception,
            WebRequest request) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(PostBookmarkNotFoundException.class)
    public ResponseEntity<ApiError> handlePostBookmarkNotFound(
            PostBookmarkNotFoundException exception,
            WebRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ApiError> handleNotificationNotFound(
            NotificationNotFoundException exception,
            WebRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(InvalidSearchQueryException.class)
    public ResponseEntity<ApiError> handleInvalidSearchQuery(
            InvalidSearchQueryException exception,
            WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(InvalidStoryException.class)
    public ResponseEntity<ApiError> handleInvalidStory(InvalidStoryException exception, WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(StoryNotFoundException.class)
    public ResponseEntity<ApiError> handleStoryNotFound(StoryNotFoundException exception, WebRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(InvalidHashtagException.class)
    public ResponseEntity<ApiError> handleInvalidHashtag(InvalidHashtagException exception, WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(HashtagNotFoundException.class)
    public ResponseEntity<ApiError> handleHashtagNotFound(HashtagNotFoundException exception, WebRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(AiProviderException.class)
    public ResponseEntity<ApiError> handleAiProviderFailure(
            AiProviderException exception,
            WebRequest request) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(ModelRoutingException.class)
    public ResponseEntity<ApiError> handleModelRouting(ModelRoutingException exception, WebRequest request) {
        HttpStatus status = switch (exception.getCode()) {
            case "MODEL_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "MODEL_REQUIRED", "INVALID_SELECTION_MODE", "CAPABILITY_MISMATCH" -> HttpStatus.BAD_REQUEST;
            case "MODEL_COMING_SOON" -> HttpStatus.CONFLICT;
            default -> HttpStatus.SERVICE_UNAVAILABLE;
        };
        return buildResponse(status, exception.getMessage(), request, Map.of("code", exception.getCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception,
            WebRequest request) {
        Map<String, String> validationErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                validationErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        return buildResponse(HttpStatus.BAD_REQUEST, "Request validation failed", request, validationErrors);
    }

    private ResponseEntity<ApiError> buildResponse(
            HttpStatus status,
            String message,
            WebRequest request,
            Map<String, String> validationErrors) {
        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getDescription(false).replace("uri=", ""),
                validationErrors);

        return ResponseEntity.status(status).body(error);
    }
}
