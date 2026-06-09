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
    public ResponseEntity<CheckoutResposta> contratarPlano(@Valid @RequestBody CheckoutRequisicao req,
                                                           @AuthenticationPrincipal UserDetails principal,
                                                           HttpServletRequest http) {
        String ip = extrairIp(http);
        CheckoutResposta resp = assinaturaServico.contratarPlano(req, principal.getUsername(), ip);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping("/meu-plano")
    public ResponseEntity<StatusPagamentoResposta> meuPlano(@AuthenticationPrincipal UserDetails principal,
                                                            HttpServletRequest http) {
        try {
            return ResponseEntity.ok(assinaturaServico.buscarStatusPorUsuario(principal.getUsername(), extrairIp(http)));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("Nenhuma transação")) {
            return ResponseEntity.noContent().build();
        }
            throw e;
        }
    }

    @GetMapping("/pagamentos/{asaasPaymentId}/status")
    public ResponseEntity<StatusPagamentoResposta> sincronizarStatus(@PathVariable String asaasPaymentId,
                                                                     @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(assinaturaServico.sincronizarStatus(principal.getUsername(), asaasPaymentId));
    }

    @DeleteMapping("/cancelar")
    public ResponseEntity<Void> cancelar(@AuthenticationPrincipal UserDetails principal,
                                         HttpServletRequest http) {
        assinaturaServico.cancelarAssinatura(principal.getUsername(), extrairIp(http));
        return ResponseEntity.noContent().build();
    }

    private String extrairIp(HttpServletRequest req) {
        String xf = req.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) return xf.split(",")[0].trim();
        return req.getRemoteAddr();
        }
    }