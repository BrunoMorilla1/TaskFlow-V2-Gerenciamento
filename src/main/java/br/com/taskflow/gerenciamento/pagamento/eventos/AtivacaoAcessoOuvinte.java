package br.com.taskflow.gerenciamento.pagamento.eventos;

import br.com.taskflow.gerenciamento.pagamento.entidade.Assinatura;
import br.com.taskflow.gerenciamento.usuarios.repositorio.UsuarioRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AtivacaoAcessoOuvinte {

    private final UsuarioRepositorio usuarioRepositorio;

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void aoConfirmarPagamento(PagamentoConfirmadoEvento evento) {
        Assinatura assinatura = evento.getAssinatura();
        String ip = evento.getIp();
        var usuario = assinatura.getUsuario();

        log.info("Ouvinte[Acesso] - Iniciando liberacao de recursos. usuarioId={}, assinaturaId={}, ip={}",
                usuario.getId(), assinatura.getId(), ip);

        try {
            if (usuario.isAtivo() && "PRODUCAO_ELITE".equals(usuario.getTipoPlano())) {
                log.info("Ouvinte[Acesso] - Usuario ja possui acesso ativo. Ignorando duplicidade. usuarioId={}", usuario.getId());
                return;
            }

            usuario.setAtivo(true);
            usuario.setTipoPlano("PRODUCAO_ELITE");

            usuarioRepositorio.saveAndFlush(usuario);

            log.info("Ouvinte[Acesso] - Recursos liberados com sucesso. usuarioId={}, plano=PRODUCAO_ELITE, ip={}",
                    usuario.getId(), ip);

        } catch (Exception e) {
            log.error("Ouvinte[Acesso] - ERRO CRITICO ao liberar recursos. usuarioId={}, ip={}, erro={}",
                    usuario.getId(), ip, e.getMessage());

            throw e;
        }
    }
}