package br.com.taskflow.gerenciamento.excecao;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ApiErro> tratarNegocioException(NegocioException ex, HttpServletRequest request) {
        log.warn("NegocioException [{}]: {} no path {}", ex.getCodigo(), ex.getMessage(), request.getRequestURI());

        ApiErro erro = ApiErro.of(
                ex.getStatus().value(),
                ex.getStatus().name(),
                ex.getCodigo(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(ex.getStatus()).body(erro);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErro> tratarValidacao(MethodArgumentNotValidException ex, HttpServletRequest request) {
        var erros = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        err -> err.getField(),
                        err -> err.getDefaultMessage(),
                        (existente, novo) -> existente
                ));

        log.warn("Falha de Validação: {} no path {}", erros, request.getRequestURI());

        ApiErro erro = ApiErro.comValidacao(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.name(),
                "ERRO_VALIDACAO",
                "Dados de entrada inválidos.",
                request.getRequestURI(),
                erros
        );

        return ResponseEntity.badRequest().body(erro);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErro> tratarBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Tentativa de login inválida no path {}", request.getRequestURI());

        ApiErro erro = ApiErro.of(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.name(),
                "AUTH_CREDENCIAIS_INVALIDAS",
                "Email ou senha incorretos.",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(erro);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErro> tratarAcessoNegado(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Acesso negado no path {}", request.getRequestURI());

        ApiErro erro = ApiErro.of(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.name(),
                "ACESSO_NEGADO",
                "Você não tem permissão para realizar esta ação.",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(erro);
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiErro> tratarBadRequest(Exception ex, HttpServletRequest request) {
        log.warn("Requisição malformada: {} no path {}", ex.getMessage(), request.getRequestURI());

        ApiErro erro = ApiErro.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.name(),
                "REQUISICAO_INVALIDA",
                "Dados enviados de forma incorreta ou ausentes.",
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(erro);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErro> tratarErroGenerico(Exception ex, HttpServletRequest request) {
        log.error("ERRO CRÍTICO NÃO TRATADO: {} no path {}", ex.getClass().getSimpleName(), request.getRequestURI(), ex);

        ApiErro erro = ApiErro.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.name(),
                "ERRO_INTERNO_SERVIDOR",
                "Ocorreu um erro inesperado. O suporte técnico foi notificado.",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
    }
}