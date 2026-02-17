package com.labOS.backend.exception;

import com.labOS.backend.common.BaseResponse;
import com.labOS.backend.common.ErrorCode;
import com.labOS.backend.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.sql.SQLException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Global exception handler
 * Catches and handles all exceptions thrown in the application
 * Ensures no stack trace is exposed to clients
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle custom business exceptions
     * These are expected exceptions, only log message without stack trace
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.warn("BusinessException: code={}, message={}", e.getCode(), e.getMessage());
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    /**
     * Handle validation exceptions from @Valid annotation
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public BaseResponse<?> methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        log.warn("Validation failed: {}", e.getMessage());
        StringBuilder errorMessage = new StringBuilder("Validation failed: ");
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            errorMessage.append(error.getField())
                    .append(" - ")
                    .append(error.getDefaultMessage())
                    .append("; ");
        }
        String message = errorMessage.toString();
        log.debug("Validation details: {}", message);
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, message);
    }

    /**
     * Handle binding exceptions
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public BaseResponse<?> bindExceptionHandler(BindException e) {
        log.warn("Binding error: {}", e.getMessage());
        StringBuilder errorMessage = new StringBuilder("Validation failed: ");
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            errorMessage.append(error.getField())
                    .append(" - ")
                    .append(error.getDefaultMessage())
                    .append("; ");
        }
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, errorMessage.toString());
    }

    /**
     * Handle constraint violation exceptions
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public BaseResponse<?> constraintViolationExceptionHandler(ConstraintViolationException e) {
        log.warn("Constraint violation: {}", e.getMessage());
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        String errorMessage = violations.stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, "Validation failed: " + errorMessage);
    }

    /**
     * Handle missing request parameters
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public BaseResponse<?> missingServletRequestParameterExceptionHandler(MissingServletRequestParameterException e) {
        log.warn("Missing request parameter: {}", e.getParameterName());
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, 
                "Missing required parameter: " + e.getParameterName());
    }

    /**
     * Handle method argument type mismatch
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public BaseResponse<?> methodArgumentTypeMismatchExceptionHandler(MethodArgumentTypeMismatchException e) {
        log.warn("Type mismatch for parameter: {}", e.getName());
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, 
                "Invalid parameter type for: " + e.getName());
    }

    /**
     * Handle HTTP message not readable (malformed JSON, etc.)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public BaseResponse<?> httpMessageNotReadableExceptionHandler(HttpMessageNotReadableException e) {
        log.warn("Invalid request body format: {}", e.getMessage());
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, "Invalid request format");
    }

    /**
     * Handle unsupported HTTP method
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public BaseResponse<?> httpRequestMethodNotSupportedExceptionHandler(HttpRequestMethodNotSupportedException e) {
        log.warn("Unsupported HTTP method: {}", e.getMethod());
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, "HTTP method not supported");
    }

    /**
     * Handle file upload size exceeded
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public BaseResponse<?> maxUploadSizeExceededExceptionHandler(MaxUploadSizeExceededException e) {
        log.warn("File upload size exceeded: {}", e.getMessage());
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, "File size exceeds maximum limit");
    }

    /**
     * Handle 404 not found
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public BaseResponse<?> noHandlerFoundExceptionHandler(NoHandlerFoundException e) {
        log.warn("Handler not found: {}", e.getRequestURL());
        return ResultUtils.error(ErrorCode.NOT_FOUND_ERROR, "Requested resource not found");
    }

    /**
     * Handle SQL exceptions
     * Log full details but don't expose to client
     */
    @ExceptionHandler(SQLException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public BaseResponse<?> sqlExceptionHandler(SQLException e) {
        log.error("SQLException: errorCode={}, sqlState={}, message={}", 
                e.getErrorCode(), e.getSQLState(), e.getMessage());
        // Only log stack trace in debug mode, don't expose to client
        if (log.isDebugEnabled()) {
            log.debug("SQLException stack trace", e);
        }
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "Database operation failed");
    }

    /**
     * Handle data access exceptions
     * Log full details but don't expose to client
     */
    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public BaseResponse<?> dataAccessExceptionHandler(DataAccessException e) {
        log.error("DataAccessException: message={}", e.getMessage());
        // Only log stack trace in debug mode
        if (log.isDebugEnabled()) {
            log.debug("DataAccessException stack trace", e);
        }
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "Database access error");
    }

    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public BaseResponse<?> illegalArgumentExceptionHandler(IllegalArgumentException e) {
        log.warn("IllegalArgumentException: {}", e.getMessage());
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, e.getMessage());
    }

    /**
     * Handle illegal state exceptions
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public BaseResponse<?> illegalStateExceptionHandler(IllegalStateException e) {
        log.warn("IllegalStateException: {}", e.getMessage());
        return ResultUtils.error(ErrorCode.OPERATION_ERROR, e.getMessage());
    }

    /**
     * Handle null pointer exceptions
     * Log full details but don't expose to client
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public BaseResponse<?> nullPointerExceptionHandler(NullPointerException e) {
        log.error("NullPointerException: {}", e.getMessage());
        // Only log stack trace in debug mode
        if (log.isDebugEnabled()) {
            log.debug("NullPointerException stack trace", e);
        }
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "System error occurred");
    }

    /**
     * Handle runtime exceptions (catch-all for RuntimeException)
     * Log full details but don't expose internal details to client
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException: type={}, message={}", 
                e.getClass().getSimpleName(), e.getMessage());
        // Only log stack trace in debug mode
        if (log.isDebugEnabled()) {
            log.debug("RuntimeException stack trace", e);
        }
        // Don't expose internal error message to client
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "System error occurred");
    }

    /**
     * Handle all other exceptions (final catch-all)
     * Never expose stack trace or internal details to client
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public BaseResponse<?> exceptionHandler(Exception e) {
        log.error("Unexpected Exception: type={}, message={}", 
                e.getClass().getSimpleName(), e.getMessage());
        // Only log stack trace in debug mode
        if (log.isDebugEnabled()) {
            log.debug("Unexpected Exception stack trace", e);
        }
        // Never expose internal error details to client
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "An unexpected error occurred");
    }
}
