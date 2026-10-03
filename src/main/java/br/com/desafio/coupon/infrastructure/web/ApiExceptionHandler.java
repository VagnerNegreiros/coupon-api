package br.com.desafio.coupon.infrastructure.web;

import br.com.desafio.coupon.application.exception.ConcurrentCouponModificationException;
import br.com.desafio.coupon.application.exception.CouponNotFoundException;
import br.com.desafio.coupon.domain.exception.CouponAlreadyDeletedException;
import br.com.desafio.coupon.domain.exception.InvalidCouponException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;

import java.util.Optional;

/**
 * Traduz exceções de domínio/aplicação para respostas HTTP no formato Problem Details (RFC 9457).
 * É o único ponto que conhece a relação "regra violada → status HTTP".
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(InvalidCouponException.class)
    ProblemDetail handleInvalidCoupon(InvalidCouponException ex) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Dados do cupom inválidos", ex.getMessage());
        problem.setProperty("field", ex.getField());
        return problem;
    }

    @ExceptionHandler(CouponNotFoundException.class)
    ProblemDetail handleNotFound(CouponNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Cupom não encontrado", ex.getMessage());
    }

    @ExceptionHandler(CouponAlreadyDeletedException.class)
    ProblemDetail handleAlreadyDeleted(CouponAlreadyDeletedException ex) {
        return problem(HttpStatus.CONFLICT, "Cupom já deletado", ex.getMessage());
    }

    @ExceptionHandler(ConcurrentCouponModificationException.class)
    ProblemDetail handleConcurrentModification(ConcurrentCouponModificationException ex) {
        return problem(HttpStatus.CONFLICT, "Conflito de concorrência", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Erro inesperado", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro inesperado. Tente novamente mais tarde.");
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        String detail = invalidFieldName(ex)
                .map(ApiExceptionHandler::invalidFieldMessage)
                .orElse("Corpo da requisição ausente ou malformado.");
        return handleExceptionInternal(ex, problem(HttpStatus.BAD_REQUEST, "Requisição inválida", detail),
                headers, status, request);
    }

    private static Optional<String> invalidFieldName(Throwable ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof JacksonException jsonError && !jsonError.getPath().isEmpty()) {
                return Optional.ofNullable(jsonError.getPath().getLast().getPropertyName());
            }
        }
        return Optional.empty();
    }

    private static String invalidFieldMessage(String field) {
        String message = "Valor inválido para o campo '%s'.".formatted(field);
        return "expirationDate".equals(field)
                ? message + " Use o formato ISO-8601 (ex.: 2030-12-31T23:59:59Z)."
                : message;
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
                                                        HttpStatusCode status, WebRequest request) {
        String detail = "Valor '%s' inválido para o parâmetro '%s'.".formatted(ex.getValue(), ex.getPropertyName());
        return handleExceptionInternal(ex, problem(HttpStatus.BAD_REQUEST, "Parâmetro inválido", detail),
                headers, status, request);
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
