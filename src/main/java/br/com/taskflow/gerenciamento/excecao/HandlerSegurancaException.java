package br.com.taskflow.gerenciamento.excecao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class HandlerSegurancaException implements AuthenticationEntryPoint {

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response,
                       AuthenticationException authException) throws IOException {

    enviarErro(request, response, HttpStatus.UNAUTHORIZED, "AUTH_NECESSARIA",
            "Acesso negado. Você precisa estar autenticado.");
  }

  public void tratarTokenInvalido(HttpServletRequest request, HttpServletResponse response) throws IOException {
    enviarErro(request, response, HttpStatus.UNAUTHORIZED, "TOKEN_INVALIDO",
            "Sessão expirada ou token inválido. Por favor, faça login novamente.");
  }

  private void enviarErro(HttpServletRequest request, HttpServletResponse response,
                          HttpStatus status, String codigo, String mensagem) throws IOException {

    ApiErro erro = ApiErro.of(
            status.value(),
            status.name(),
            codigo,
            mensagem,
            request.getRequestURI()
    );

    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    response.setStatus(status.value());

    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    response.getWriter().write(mapper.writeValueAsString(erro));
  }
}