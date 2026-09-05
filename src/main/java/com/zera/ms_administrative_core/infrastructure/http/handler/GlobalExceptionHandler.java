package com.zera.ms_administrative_core.infrastructure.http.handler;

import com.zera.ms_administrative_core.core.domain.exception.CnpjAlreadyInUseException;
import com.zera.ms_administrative_core.core.domain.exception.EmailAlreadyInUseException;
import com.zera.ms_administrative_core.core.domain.exception.InvalidCepException;
import com.zera.ms_administrative_core.core.domain.exception.InvalidCnpjException;
import com.zera.ms_administrative_core.core.domain.exception.InvalidCredentialsException;
import com.zera.ms_administrative_core.core.domain.exception.InvalidRefreshTokenException;
import com.zera.ms_administrative_core.core.domain.exception.InvalidStatusTransitionException;
import com.zera.ms_administrative_core.core.domain.exception.InvalidTelephoneNumberException;
import com.zera.ms_administrative_core.core.domain.exception.InvitationExpiredException;
import com.zera.ms_administrative_core.core.domain.exception.InvitationNotFoundException;
import com.zera.ms_administrative_core.core.domain.exception.OrganizationNotFoundException;
import com.zera.ms_administrative_core.core.domain.exception.RecyclingNotFoundException;
import com.zera.ms_administrative_core.core.domain.exception.TelephoneAlreadyRegisteredException;
import com.zera.ms_administrative_core.core.domain.exception.TelephoneNotFoundException;
import com.zera.ms_administrative_core.core.domain.exception.UnitNotFoundException;
import com.zera.ms_administrative_core.core.domain.exception.UserNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // --- 404 ---

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(OrganizationNotFoundException.class)
    public ProblemDetail handleOrganizationNotFound(OrganizationNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(UnitNotFoundException.class)
    public ProblemDetail handleUnitNotFound(UnitNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TelephoneNotFoundException.class)
    public ProblemDetail handleTelephoneNotFound(TelephoneNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvitationNotFoundException.class)
    public ProblemDetail handleInvitationNotFound(InvitationNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(RecyclingNotFoundException.class)
    public ProblemDetail handleRecyclingNotFound(RecyclingNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // --- 400 ---

    @ExceptionHandler(InvalidCnpjException.class)
    public ProblemDetail handleInvalidCnpj(InvalidCnpjException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidCepException.class)
    public ProblemDetail handleInvalidCep(InvalidCepException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidTelephoneNumberException.class)
    public ProblemDetail handleInvalidTelephoneNumber(InvalidTelephoneNumberException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(java.util.stream.Collectors.joining(", "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String detail = "Parametro '" + ex.getName() + "' com valor invalido";
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Corpo da requisicao invalido ou ausente");
    }

    // --- 401 / 403 ---

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ProblemDetail handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ProblemDetail handleAccessDenied(AuthorizationDeniedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Acesso negado");
    }

    // --- 409 ---

    @ExceptionHandler(EmailAlreadyInUseException.class)
    public ProblemDetail handleEmailInUse(EmailAlreadyInUseException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(CnpjAlreadyInUseException.class)
    public ProblemDetail handleCnpjInUse(CnpjAlreadyInUseException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(TelephoneAlreadyRegisteredException.class)
    public ProblemDetail handleTelephoneAlreadyRegistered(TelephoneAlreadyRegisteredException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    // --- 422 ---

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ProblemDetail handleInvalidTransition(InvalidStatusTransitionException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(InvitationExpiredException.class)
    public ProblemDetail handleInvitationExpired(InvitationExpiredException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    // --- fallback ---

    /**
     * Rede de seguranca: qualquer excecao nao mapeada acima. Excecoes do proprio Spring MVC
     * (404 de rota, 405, 415, etc.) implementam {@link ErrorResponse} e mantem o status correto;
     * o resto vira 500 com corpo generico (o detalhe fica so no log).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex) {
        if (ex instanceof ErrorResponse errorResponse) {
            return ResponseEntity.status(errorResponse.getStatusCode()).body(errorResponse.getBody());
        }
        log.error("Excecao nao tratada", ex);
        return ResponseEntity.internalServerError()
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno"));
    }
}
