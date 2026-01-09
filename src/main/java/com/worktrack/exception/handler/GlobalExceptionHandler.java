package com.worktrack.exception.handler;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.worktrack.dto.response.ErrorCode;
import com.worktrack.dto.response.ErrorResponse;
import com.worktrack.exception.EntityNotFoundException;
import com.worktrack.exception.ErrorMessages;
import com.worktrack.exception.FileStorageException;
import com.worktrack.exception.auth.AuthenticationException;
import com.worktrack.exception.auth.InvalidCredentialsException;
import com.worktrack.exception.user.DuplicateUserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Arrays;
import java.util.stream.Collectors;

import static com.worktrack.dto.response.ErrorCode.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleAuthentication(AuthenticationException ex) {
        logger.info("Authentication failed", ex);
        return buildResponse(AUTHENTICATION_FAILED, "Authentication failed. Please try again.");
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleInvalidCredentials(InvalidCredentialsException ex) {
        logger.info("Invalid credentials", ex);
        return buildResponse(INVALID_CREDENTIAL, "Invalid username or password.");
    }


    @ExceptionHandler(DuplicateUserException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleDuplicateUser(DuplicateUserException ex) {
        logger.info("Duplicate user", ex);
        return buildResponse(DUPLICATE_USER, "User already exists with the given credentials.");
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleEntityNotFound(EntityNotFoundException ex) {
        logger.info("Entity not found", ex);
        return buildResponse(ENTITY_NOT_FOUND, "The requested resource was not found.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((m1, m2) -> m1 + "; " + m2)
                .orElse(ErrorMessages.VALIDATION_FAILED);
        logger.info("Validation failed: {}", message);

        return buildResponse(VALIDATION_ERROR, message);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        logger.error("Data integrity violation", ex);
        return buildResponse(DB_INTEGRITY, "Data integrity violation");
    }

    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicateKey(DuplicateKeyException ex) {
        logger.error("Duplicate key error", ex);
        return buildResponse(DB_DUPLICATE_KEY, "Duplicate key error");
    }

    @ExceptionHandler(CannotAcquireLockException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleAcquireLock(CannotAcquireLockException ex) {
        logger.error("Cannot acquire lock", ex);
        return buildResponse(DB_ACQUIRE_LOCK, "Database is temporarily unavailable. Please try again later.");
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAuthorizationDenied(AuthorizationDeniedException ex) {
        logger.info("Access denied", ex);
        return buildResponse(ACCESS_DENIED, ErrorMessages.ACCESS_DENIED);
    }

    @ExceptionHandler(QueryTimeoutException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleQueryTimeout(QueryTimeoutException ex) {
        logger.warn("Database query timeout");
        return buildResponse(DB_QUERY_TIMEOUT, "Database temporarily unavailable");
    }

    @ExceptionHandler(FileStorageException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleFileStorage(FileStorageException ex) {
        logger.error("File storage error", ex);
        return buildResponse(FILE_STORAGE_ERROR, "File storage operation failed. Please try again.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getMostSpecificCause();
        String message = "Malformed JSON or incompatible types";
        if (cause instanceof InvalidFormatException invalidFormatException) {
            var targetType = invalidFormatException.getTargetType();
            if (targetType != null && targetType.isEnum()) {
                String invalidValue = String.valueOf(invalidFormatException.getValue());
                String allowed = Arrays.stream(targetType.getEnumConstants())
                        .map(Object::toString)
                        .collect(Collectors.joining(", "));
                message = "Invalid value '" + invalidValue + "' for field " + pathOf(invalidFormatException)
                        + ". Allowed values: " + allowed;
            }
        }
        logger.info(message);
        return buildResponse(ErrorCode.VALIDATION_ERROR, message);
    }

    private String pathOf(InvalidFormatException ex) {
        var ref = ex.getPath();
        if (ref == null || ref.isEmpty()) return "body";
        return ref.stream()
                .map(p -> p.getFieldName() != null ? p.getFieldName() : ("[" + p.getIndex() + "]"))
                .collect(Collectors.joining("."));
    }


    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleJpaConstraint(jakarta.validation.ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        logger.info("Persist-time validation failed: {}", message);
        return buildResponse(VALIDATION_ERROR, message);
    }


    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        var target = ex.getRequiredType();
        String message;
        if (target != null && target.isEnum()) {
            String allowed = Arrays.stream(target.getEnumConstants()).map(Object::toString).collect(Collectors.joining(", "));
            message = "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'. Allowed: " + allowed;
        } else {
            String expectedType = (target != null) ? target.getSimpleName() : "unknown";
            message = "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'. Expected type: " + expectedType;
        }
        logger.info("Type mismatch: {}", message);
        return buildResponse(VALIDATION_ERROR, message);
    }


    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneric(Exception ex) {
        logger.error("Unknown error occurred", ex);
        return buildResponse(INTERNAL_ERROR, ErrorMessages.UNKNOWN_ERROR);
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingParam(org.springframework.web.bind.MissingServletRequestParameterException ex) {
        return buildResponse(VALIDATION_ERROR, "Missing required parameter: " + ex.getParameterName());
    }

    private static ErrorResponse buildResponse(ErrorCode code, String message) {
        return new ErrorResponse(code, message);
    }

}
