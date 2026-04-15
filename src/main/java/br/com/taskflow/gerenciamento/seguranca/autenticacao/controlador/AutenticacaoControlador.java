package br.com.taskflow.gerenciamento.seguranca.autenticacao.controlador;

import br.com.taskflow.gerenciamento.seguranca.autenticacao.dto.requisicao.LoginRequisicao;
import br.com.taskflow.gerenciamento.seguranca.autenticacao.dto.requisicao.RefreshTokenRequisicao;
import br.com.taskflow.gerenciamento.seguranca.autenticacao.dto.resposta.LoginResposta;
import br.com.taskflow.gerenciamento.seguranca.autenticacao.servico.ServicoAutenticacao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AutenticacaoControlador {

    private final ServicoAutenticacao servicoAutenticacao;

    @PostMapping("/login")
    public ResponseEntity<LoginResposta> login(
            @Valid @RequestBody LoginRequisicao requisicao,
            HttpServletRequest request
    ) {
        log.info("AuthController - Recebida requisição de login. email={}", requisicao.emailNormalizado());

        LoginResposta resposta = servicoAutenticacao.autenticar(requisicao, request);

        log.info("AuthController - Login realizado com sucesso. email={}", requisicao.emailNormalizado());

        return ResponseEntity.ok(resposta);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResposta> refreshToken(
            @Valid @RequestBody RefreshTokenRequisicao requisicao,
            HttpServletRequest request
    ) {
        log.info("AuthController - Recebida requisição de renovação de token.");

        LoginResposta resposta = servicoAutenticacao.renovarToken(requisicao, request);

        log.info("AuthController - Token renovado com sucesso.");

        return ResponseEntity.ok(resposta);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
    ) {
        log.info("AuthController - Recebida requisição de logout.");

        servicoAutenticacao.registrarLogout(authorizationHeader);

        log.info("AuthController - Logout processado com sucesso.");

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}