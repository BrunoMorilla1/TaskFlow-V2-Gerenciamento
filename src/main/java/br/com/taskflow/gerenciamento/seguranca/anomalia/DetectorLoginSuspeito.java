package br.com.taskflow.gerenciamento.seguranca.anomalia;

import br.com.taskflow.gerenciamento.seguranca.auditoria.ServicoAuditoriaSeguranca;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class DetectorLoginSuspeito {

    private static final int LIMITE_LOGINS_JANELA = 5;
    private static final int JANELA_SEGUNDOS = 60;
    private static final int TEMPO_EXPIRACAO_REGISTRO_SEGUNDOS = 86400; // 24h

    private static final String EVENTO_LOGIN_IP_DIFERENTE = "LOGIN_IP_DIFERENTE";
    private static final String EVENTO_LOGIN_DEVICE_DIFERENTE = "LOGIN_DEVICE_DIFERENTE";
    private static final String EVENTO_LOGIN_MUITAS_TENTATIVAS = "LOGIN_MUITAS_TENTATIVAS";

    private final ServicoAuditoriaSeguranca servicoAuditoriaSeguranca;

    private final Map<String, RegistroLogin> historicoLoginPorEmail = new ConcurrentHashMap<>();

    public void analisarLogin(String email, String ip, String userAgent) {
        String emailNormalizado = normalizarEmail(email);
        String ipNormalizado = normalizarIp(ip);
        String userAgentNormalizado = normalizarUserAgent(userAgent);

        if (emailNormalizado == null || emailNormalizado.isBlank()) {
            log.warn("DetectorLoginSuspeito - Email inválido ao analisar login suspeito.");
            return;
        }

        Instant agora = Instant.now();

        historicoLoginPorEmail.compute(emailNormalizado, (chave, registroAtual) -> {
            if (registroAtual == null || registroAtual.expirado(agora)) {
                log.debug("DetectorLoginSuspeito - Criando novo registro de login. email={}", emailNormalizado);
                return RegistroLogin.novo(ipNormalizado, userAgentNormalizado, agora);
            }

            if (registroAtual.ipDiferente(ipNormalizado)) {
                log.warn("DetectorLoginSuspeito - Login suspeito por IP diferente. email={}, ipAnterior={}, ipAtual={}",
                        emailNormalizado, registroAtual.getIp(), ipNormalizado);

                servicoAuditoriaSeguranca.registrarEventoSuspeito(
                        EVENTO_LOGIN_IP_DIFERENTE,
                        emailNormalizado,
                        ipNormalizado,
                        "IP diferente do último login."
                );
            }

            if (registroAtual.userAgentDiferente(userAgentNormalizado)) {
                log.warn("DetectorLoginSuspeito - Login suspeito por dispositivo diferente. email={}",
                        emailNormalizado);

                servicoAuditoriaSeguranca.registrarEventoSuspeito(
                        EVENTO_LOGIN_DEVICE_DIFERENTE,
                        emailNormalizado,
                        ipNormalizado,
                        "Dispositivo diferente detectado."
                );
            }

            if (registroAtual.estaNaMesmaJanela(agora, JANELA_SEGUNDOS)) {
                registroAtual.incrementarContador();

                if (registroAtual.getContador() >= LIMITE_LOGINS_JANELA) {
                    log.warn("DetectorLoginSuspeito - Múltiplos logins em curto período. email={}, ip={}, contador={}",
                            emailNormalizado, ipNormalizado, registroAtual.getContador());

                    servicoAuditoriaSeguranca.registrarEventoSuspeito(
                            EVENTO_LOGIN_MUITAS_TENTATIVAS,
                            emailNormalizado,
                            ipNormalizado,
                            "Múltiplos logins em curto período."
                    );
                }
            } else {
                registroAtual.reiniciarJanela(agora);
            }

            registroAtual.atualizar(ipNormalizado, userAgentNormalizado, agora);
            return registroAtual;
        });
    }

    @Scheduled(fixedDelay = 600_000)
    public void limparHistoricoAntigo() {
        Instant agora = Instant.now();
        int tamanhoAntes = historicoLoginPorEmail.size();

        historicoLoginPorEmail.entrySet().removeIf(entry -> entry.getValue().expirado(agora));

        int removidos = tamanhoAntes - historicoLoginPorEmail.size();

        if (removidos > 0) {
            log.debug("DetectorLoginSuspeito - Registros antigos removidos. total={}", removidos);
        }
    }

    public int quantidadeRegistros() {
        return historicoLoginPorEmail.size();
    }

    private String normalizarEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    private String normalizarIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "desconhecido";
        }
        return ip.trim();
    }

    private String normalizarUserAgent(String userAgent) {
        if (userAgent == null) {
            return null;
        }

        String normalizado = userAgent.trim();
        return normalizado.isBlank() ? null : normalizado;
    }

    private static final class RegistroLogin {

        private String ip;
        private String userAgent;
        private Instant ultimoLogin;
        private Instant inicioJanela;
        private int contador;

        private RegistroLogin(String ip, String userAgent, Instant ultimoLogin, Instant inicioJanela, int contador) {
            this.ip = ip;
            this.userAgent = userAgent;
            this.ultimoLogin = ultimoLogin;
            this.inicioJanela = inicioJanela;
            this.contador = contador;
        }

        private static RegistroLogin novo(String ip, String userAgent, Instant agora) {
            return new RegistroLogin(ip, userAgent, agora, agora, 1);
        }

        private boolean ipDiferente(String novoIp) {
            return this.ip != null && novoIp != null && !this.ip.equals(novoIp);
        }

        private boolean userAgentDiferente(String novoUserAgent) {
            return this.userAgent != null
                    && novoUserAgent != null
                    && !this.userAgent.equals(novoUserAgent);
        }

        private boolean estaNaMesmaJanela(Instant agora, int janelaSegundos) {
            return agora.isBefore(this.inicioJanela.plusSeconds(janelaSegundos));
        }

        private void incrementarContador() {
            this.contador++;
        }

        private void reiniciarJanela(Instant agora) {
            this.inicioJanela = agora;
            this.contador = 1;
        }

        private void atualizar(String ip, String userAgent, Instant agora) {
            this.ip = ip;
            this.userAgent = userAgent;
            this.ultimoLogin = agora;
        }

        private boolean expirado(Instant agora) {
            return ultimoLogin != null && agora.isAfter(ultimoLogin.plusSeconds(TEMPO_EXPIRACAO_REGISTRO_SEGUNDOS));
        }

        private String getIp() {
            return ip;
        }

        private int getContador() {
            return contador;
        }
    }
}