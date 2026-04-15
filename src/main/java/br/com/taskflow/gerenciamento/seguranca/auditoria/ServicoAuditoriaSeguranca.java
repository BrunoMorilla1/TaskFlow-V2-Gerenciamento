package br.com.taskflow.gerenciamento.seguranca.auditoria;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServicoAuditoriaSeguranca {

    private static final String EVENTO_LOGIN_SUCESSO = "LOGIN_SUCESSO";
    private static final String EVENTO_LOGIN_FALHA = "LOGIN_FALHA";
    private static final String EVENTO_LOGOUT = "LOGOUT";
    private static final String DETALHE_CREDENCIAIS_INVALIDAS = "Credenciais inválidas";

    private final RepositorioLogSeguranca repositorioLogSeguranca;

    @Transactional
    public void registrarLoginSucesso(String email, String ip, String userAgent) {
        EntidadeLogSeguranca entidade = criarEvento(
                EVENTO_LOGIN_SUCESSO,
                email,
                ip,
                userAgent,
                null
        );

        repositorioLogSeguranca.save(entidade);

        log.info("ServicoAuditoriaSeguranca - Login com sucesso registrado. email={}, ip={}",
                entidade.getEmailUsuario(), entidade.getIp());
    }

    @Transactional
    public void registrarLoginFalha(String email, String ip, String userAgent) {
        EntidadeLogSeguranca entidade = criarEvento(
                EVENTO_LOGIN_FALHA,
                email,
                ip,
                userAgent,
                DETALHE_CREDENCIAIS_INVALIDAS
        );

        repositorioLogSeguranca.save(entidade);

        log.warn("ServicoAuditoriaSeguranca - Falha de login registrada. email={}, ip={}",
                entidade.getEmailUsuario(), entidade.getIp());
    }

    @Transactional
    public void registrarLogout(String email, String ip) {
        EntidadeLogSeguranca entidade = criarEvento(
                EVENTO_LOGOUT,
                email,
                ip,
                null,
                null
        );

        repositorioLogSeguranca.save(entidade);

        log.info("ServicoAuditoriaSeguranca - Logout registrado. email={}, ip={}",
                entidade.getEmailUsuario(), entidade.getIp());
    }

    @Transactional
    public void registrarEventoSuspeito(String tipoEvento, String email, String ip, String detalhes) {
        String tipoEventoNormalizado = normalizarObrigatorio(tipoEvento, "O tipo do evento é obrigatório.");

        EntidadeLogSeguranca entidade = criarEvento(
                tipoEventoNormalizado,
                email,
                ip,
                null,
                detalhes
        );

        repositorioLogSeguranca.save(entidade);

        log.warn("ServicoAuditoriaSeguranca - Evento suspeito registrado. tipoEvento={}, email={}, ip={}",
                entidade.getTipoEvento(), entidade.getEmailUsuario(), entidade.getIp());
    }

    private EntidadeLogSeguranca criarEvento(
            String tipoEvento,
            String email,
            String ip,
            String userAgent,
            String detalhes
    ) {
        EntidadeLogSeguranca entidade = new EntidadeLogSeguranca();

        entidade.setTipoEvento(normalizarObrigatorio(tipoEvento, "O tipo do evento é obrigatório."));
        entidade.setEmailUsuario(normalizarOpcional(email));
        entidade.setIp(normalizarIp(ip));
        entidade.setUserAgent(normalizarUserAgent(userAgent));
        entidade.setDetalhes(normalizarOpcional(detalhes));
        entidade.setDataEvento(Instant.now());

        return entidade;
    }

    private String normalizarObrigatorio(String valor, String mensagemErro) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensagemErro);
        }
        return valor.trim();
    }

    private String normalizarOpcional(String valor) {
        if (valor == null) {
            return null;
        }

        String normalizado = valor.trim();
        return normalizado.isBlank() ? null : normalizado;
    }

    private String normalizarIp(String ip) {
        String valor = normalizarOpcional(ip);
        return valor != null ? valor : "desconhecido";
    }

    private String normalizarUserAgent(String userAgent) {
        return normalizarOpcional(userAgent);
    }
}