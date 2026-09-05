package com.zera.ms_administrative_core.infrastructure.http.handler;

import com.zera.ms_administrative_core.core.domain.exception.*;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // --- UserNotFoundException ---

    @Test
    @DisplayName("Should return 404 when UserNotFoundException is thrown")
    void shouldReturn404OnUserNotFound() {
        UserNotFoundException ex = new UserNotFoundException(UUID.randomUUID());

        ProblemDetail result = handler.handleUserNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND.value(), result.getStatus());
        assertEquals(ex.getMessage(), result.getDetail());
    }

    // --- InvalidCnpjException ---

    @Test
    @DisplayName("Should return 400 with error message when InvalidCnpjException is thrown")
    void shouldReturn400OnInvalidCnpj() {
        InvalidCnpjException ex = new InvalidCnpjException("00.000.000/0000-00");

        ProblemDetail result = handler.handleInvalidCnpj(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getStatus());
        assertEquals(ex.getMessage(), result.getDetail());
    }

    // --- EmailAlreadyInUseException ---

    @Test
    @DisplayName("Should return 409 when EmailAlreadyInUseException is thrown")
    void shouldReturn409OnEmailAlreadyInUse() {
        EmailAlreadyInUseException ex = new EmailAlreadyInUseException(new Email("test@email.com"));

        ProblemDetail result = handler.handleEmailInUse(ex);

        assertEquals(HttpStatus.CONFLICT.value(), result.getStatus());
        assertEquals(ex.getMessage(), result.getDetail());
    }

    // --- InvalidStatusTransitionException ---

    @Test
    @DisplayName("Should return 422 when InvalidStatusTransitionException is thrown")
    void shouldReturn422OnInvalidStatusTransition() {
        InvalidStatusTransitionException ex = new InvalidStatusTransitionException(Status.ACTIVE, Status.ACTIVE);

        ProblemDetail result = handler.handleInvalidTransition(ex);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), result.getStatus());
        assertEquals(ex.getMessage(), result.getDetail());
    }

    // --- IllegalArgumentException ---

    @Test
    @DisplayName("Should return 400 when IllegalArgumentException is thrown")
    void shouldReturn400OnIllegalArgument() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument");

        ProblemDetail result = handler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getStatus());
        assertEquals("Invalid argument", result.getDetail());
    }

    // --- MethodArgumentNotValidException ---

    @Test
    @DisplayName("Should return 400 with field errors when MethodArgumentNotValidException is thrown")
    void shouldReturn400OnMethodArgumentNotValid() {
        BindingResult bindingResult = mock(BindingResult.class);
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
            new FieldError("request", "name", "must not be blank"),
            new FieldError("request", "email", "must be a valid email")
        ));

        ProblemDetail result = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getStatus());
        assertTrue(result.getDetail().contains("name: must not be blank"));
        assertTrue(result.getDetail().contains("email: must be a valid email"));
    }

    @Test
    @DisplayName("Should join multiple field errors with comma")
    void shouldJoinMultipleFieldErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
            new FieldError("request", "name", "must not be blank"),
            new FieldError("request", "email", "must be a valid email")
        ));

        ProblemDetail result = handler.handleValidation(ex);

        assertTrue(result.getDetail().contains(", "));
    }

    // --- InvitationNotFoundException ---

    @Test
    @DisplayName("Should return 404 when InvitationNotFoundException is thrown")
    void shouldReturn404OnInvitationNotFound() {
        InvitationNotFoundException ex = new InvitationNotFoundException("123456");

        ProblemDetail result = handler.handleInvitationNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND.value(), result.getStatus());
        assertEquals(ex.getMessage(), result.getDetail());
    }

    // --- InvitationExpiredException ---

    @Test
    @DisplayName("Should return 422 when InvitationExpiredException is thrown")
    void shouldReturn422OnInvitationExpired() {
        InvitationExpiredException ex = new InvitationExpiredException("123456");

        ProblemDetail result = handler.handleInvitationExpired(ex);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), result.getStatus());
        assertEquals(ex.getMessage(), result.getDetail());
    }

    // --- RecyclingNotFoundException ---

    @Test
    @DisplayName("Should return 404 with message when RecyclingNotFoundException is thrown")
    void shouldReturn404OnRecyclingNotFound() {
        RecyclingNotFoundException ex = new RecyclingNotFoundException(UUID.randomUUID());

        ProblemDetail result = handler.handleRecyclingNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND.value(), result.getStatus());
        assertEquals(ex.getMessage(), result.getDetail());
    }

    // --- InvalidTelephoneNumberException ---

    @Test
    @DisplayName("Should return 400 when InvalidTelephoneNumberException is thrown")
    void shouldReturn400OnInvalidTelephoneNumber() {
        InvalidTelephoneNumberException ex = new InvalidTelephoneNumberException("123");

        ProblemDetail result = handler.handleInvalidTelephoneNumber(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getStatus());
        assertEquals(ex.getMessage(), result.getDetail());
    }

    // --- OrganizationNotFoundException ---

    @Test
    @DisplayName("Should return 404 when OrganizationNotFoundException is thrown")
    void shouldReturn404OnOrganizationNotFound() {
        OrganizationNotFoundException ex = new OrganizationNotFoundException(UUID.randomUUID());

        ProblemDetail result = handler.handleOrganizationNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND.value(), result.getStatus());
        assertEquals(ex.getMessage(), result.getDetail());
    }

    // --- UnitNotFoundException ---

    @Test
    @DisplayName("Should return 404 when UnitNotFoundException is thrown")
    void shouldReturn404OnUnitNotFound() {
        UnitNotFoundException ex = new UnitNotFoundException(UUID.randomUUID());

        ProblemDetail result = handler.handleUnitNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND.value(), result.getStatus());
        assertEquals(ex.getMessage(), result.getDetail());
    }

    // --- TelephoneNotFoundException ---

    @Test
    @DisplayName("Should return 404 when TelephoneNotFoundException is thrown")
    void shouldReturn404OnTelephoneNotFound() {
        TelephoneNotFoundException ex = new TelephoneNotFoundException(UUID.randomUUID());

        ProblemDetail result = handler.handleTelephoneNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND.value(), result.getStatus());
        assertEquals(ex.getMessage(), result.getDetail());
    }

    // --- CnpjAlreadyInUseException ---

    @Test
    @DisplayName("Should return 409 when CnpjAlreadyInUseException is thrown")
    void shouldReturn409OnCnpjAlreadyInUse() {
        CnpjAlreadyInUseException ex = new CnpjAlreadyInUseException(new Cnpj("11.222.333/0001-81"));

        ProblemDetail result = handler.handleCnpjInUse(ex);

        assertEquals(HttpStatus.CONFLICT.value(), result.getStatus());
        assertEquals(ex.getMessage(), result.getDetail());
    }

    // --- TelephoneAlreadyRegisteredException ---

    @Test
    @DisplayName("Should return 409 when TelephoneAlreadyRegisteredException is thrown")
    void shouldReturn409OnTelephoneAlreadyRegistered() {
        TelephoneAlreadyRegisteredException ex =
                TelephoneAlreadyRegisteredException.forUser(UUID.randomUUID());

        ProblemDetail result = handler.handleTelephoneAlreadyRegistered(ex);

        assertEquals(HttpStatus.CONFLICT.value(), result.getStatus());
        assertEquals(ex.getMessage(), result.getDetail());
    }

    // --- InvalidCredentialsException ---

    @Test
    @DisplayName("Should return 401 when InvalidCredentialsException is thrown")
    void shouldReturn401OnInvalidCredentials() {
        InvalidCredentialsException ex = new InvalidCredentialsException();

        ProblemDetail result = handler.handleInvalidCredentials(ex);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), result.getStatus());
        assertEquals(ex.getMessage(), result.getDetail());
    }

    // --- InvalidRefreshTokenException ---

    @Test
    @DisplayName("Should return 401 when InvalidRefreshTokenException is thrown")
    void shouldReturn401OnInvalidRefreshToken() {
        InvalidRefreshTokenException ex = new InvalidRefreshTokenException();

        ProblemDetail result = handler.handleInvalidRefreshToken(ex);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), result.getStatus());
        assertEquals(ex.getMessage(), result.getDetail());
    }

    // --- InvalidCepException ---

    @Test
    @DisplayName("Should return 400 when InvalidCepException is thrown")
    void shouldReturn400OnInvalidCep() {
        InvalidCepException ex = new InvalidCepException("00000");

        ProblemDetail result = handler.handleInvalidCep(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getStatus());
        assertEquals(ex.getMessage(), result.getDetail());
    }

    // --- MethodArgumentTypeMismatchException (ex.: UUID invalido no path) ---

    @Test
    @DisplayName("Should return 400 when a path/query param has the wrong type")
    void shouldReturn400OnTypeMismatch() {
        MethodArgumentTypeMismatchException ex =
                new MethodArgumentTypeMismatchException("abc", UUID.class, "id", null, null);

        ProblemDetail result = handler.handleTypeMismatch(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getStatus());
        assertTrue(result.getDetail().contains("id"));
    }

    // --- HttpMessageNotReadableException (corpo JSON malformado) ---

    @Test
    @DisplayName("Should return 400 when the request body is unreadable")
    void shouldReturn400OnUnreadableBody() {
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("bad", (org.springframework.http.HttpInputMessage) null);

        ProblemDetail result = handler.handleUnreadableBody(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getStatus());
    }

    // --- AuthorizationDeniedException (@PreAuthorize) ---

    @Test
    @DisplayName("Should return 403 when authorization is denied")
    void shouldReturn403OnAccessDenied() {
        AuthorizationDeniedException ex = new AuthorizationDeniedException("nope",
                new org.springframework.security.authorization.AuthorizationDecision(false));

        ProblemDetail result = handler.handleAccessDenied(ex);

        assertEquals(HttpStatus.FORBIDDEN.value(), result.getStatus());
    }

    // --- fallback ---

    @Test
    @DisplayName("Unmapped exception -> 500 with a generic body")
    void shouldReturn500OnUnexpected() {
        ResponseEntity<Object> result = handler.handleUnexpected(new IllegalStateException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        assertInstanceOf(ProblemDetail.class, result.getBody());
    }

    @Test
    @DisplayName("Spring MVC ErrorResponse keeps its own status through the fallback")
    void fallbackPreservesErrorResponseStatus() {
        org.springframework.web.HttpRequestMethodNotSupportedException ex =
                new org.springframework.web.HttpRequestMethodNotSupportedException("DELETE");

        ResponseEntity<Object> result = handler.handleUnexpected(ex);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED.value(), result.getStatusCode().value());
    }
}