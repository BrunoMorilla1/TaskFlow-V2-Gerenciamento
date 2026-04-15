package br.com.taskflow.gerenciamento.pagamento.controlador;

import br.com.taskflow.gerenciamento.pagamento.dto.requisicao.CheckoutRequisicao;
import br.com.taskflow.gerenciamento.pagamento.dto.resposta.CheckoutResposta;
import br.com.taskflow.gerenciamento.pagamento.dto.resposta.StatusPagamentoResposta;
import br.com.taskflow.gerenciamento.pagamento.servico.AssinaturaServico;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/assinaturas")
@RequiredArgsConstructor
@Slf4j
public class AssinaturaControlador {

    private final AssinaturaServico assinaturaServico;

    @PostMapping("/contratar")
    public ResponseEntity<CheckoutResposta> contratarPlano(@Valid @RequestBody CheckoutRequisicao requisicao,
                                                           @AuthenticationPrincipal UserDetails principal,
                                                           HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        log.info("Controlador[Assinatura] - Requisicao para contratacao de plano 49,90. usuario={}, ip={}",
                principal.getUsername(), ip);

        CheckoutResposta response = assinaturaServico.contratarPlano(requisicao, principal.getUsername(), ip);

        log.info("Controlador[Assinatura] - Plano contratado com sucesso. assinaturaId={}, usuario={}, ip={}",
                response.assinaturaId(), principal.getUsername(), ip);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/meu-plano")
    public ResponseEntity<StatusPagamentoResposta> buscarMinhaAssinatura(@AuthenticationPrincipal UserDetails principal,
                                                                         HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        log.info("Controlador[Assinatura] - Requisicao para buscar status do plano. usuario={}, ip={}",
                principal.getUsername(), ip);

        var response = assinaturaServico.buscarStatusPorUsuario(principal.getUsername(), ip);

        log.info("Controlador[Assinatura] - Status do plano recuperado. status={}, usuario={}, ip={}",
                response.status(), principal.getUsername(), ip);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/cancelar")
    public ResponseEntity<Void> cancelarAssinatura(@AuthenticationPrincipal UserDetails principal,
                                                   HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        log.info("Controlador[Assinatura] - Requisicao para CANCELAMENTO de plano. usuario={}, ip={}",
                principal.getUsername(), ip);

        assinaturaServico.cancelarAssinatura(principal.getUsername(), ip);

        log.info("Controlador[Assinatura] - Plano cancelado com sucesso. usuario={}, ip={}",
                principal.getUsername(), ip);

        return ResponseEntity.noContent().build();
    }
}