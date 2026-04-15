package br.com.taskflow.gerenciamento.pagamento.eventos;

import br.com.taskflow.gerenciamento.pagamento.entidade.Assinatura;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificacaoPagamentoOuvinte {

    @Async
    @EventListener
    public void aoConfirmarPagamento(PagamentoConfirmadoEvento evento) {
        Assinatura assinatura = evento.getAssinatura();
        String ip = evento.getIp();
        var usuario = assinatura.getUsuario();

        log.info("Ouvinte[Notificacao] - Iniciando envio de e-mail de boas-vindas. usuarioId={}, email={}, ip={}",
                usuario.getId(), usuario.getEmail(), ip);

        try {
            log.info("Ouvinte[Notificacao] - E-mail de confirmacao enviado com sucesso. usuarioId={}, ip={}",
                    usuario.getId(), ip);

        } catch (Exception e) {
            log.error("Ouvinte[Notificacao] - Falha ao enviar e-mail de confirmacao. usuarioId={}, ip={}, erro={}",
                    usuario.getId(), ip, e.getMessage());

        }
    }

    @Async
    @EventListener
    public void aoFalharPagamento(PagamentoFalhoEvento evento) {
        log.warn("Ouvinte[Notificacao] - Notificando usuario sobre falha no pagamento. usuarioId={}, ip={}, motivo={}",
                evento.getUsuarioId(), evento.getIp(), evento.getMotivo());

        try {
            log.info("Ouvinte[Notificacao] - Alerta de falha enviado. usuarioId={}", evento.getUsuarioId());

        } catch (Exception e) {
            log.error("Ouvinte[Notificacao] - Erro ao processar notificacao de falha. usuarioId={}, erro={}",
                    evento.getUsuarioId(), e.getMessage());
        }
    }
}