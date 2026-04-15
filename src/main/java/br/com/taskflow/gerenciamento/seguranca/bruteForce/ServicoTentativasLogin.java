package br.com.taskflow.gerenciamento.seguranca.bruteForce;

import br.com.taskflow.gerenciamento.excecao.BruteForceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ServicoTentativasLogin {

    private static final int MAX_TENTATIVAS = 5;
    private static final int TEMPO_BLOQUEIO_SEGUNDOS = 300;
    private static final int TEMPO_RESET_SEGUNDOS = 900;

    private final Map<String, TentativaLogin> tentativasPorIp = new ConcurrentHashMap<>();

    public void verificarBloqueio(String ip) {
        String ipNormalizado = normalizarIp(ip);
        TentativaLogin tentativa = tentativasPorIp.get(ipNormalizado);

        if (tentativa == null) {
            return;
        }

        Instant agora = Instant.now();

        if (tentativa.estaBloqueado(agora)) {
            log.warn("ServicoTentativasLogin - IP bloqueado por brute force. ip={}, bloqueadoAte={}",
                    ipNormalizado, tentativa.getBloqueadoAte());
            throw new BruteForceException("Muitas tentativas de login. Aguarde alguns minutos.");
        }

        if (tentativa.expirouParaReset(agora)) {
            tentativasPorIp.remove(ipNormalizado);
            log.debug("ServicoTentativasLogin - Tentativas expiradas removidas. ip={}", ipNormalizado);
        }
    }

    public void registrarFalha(String ip) {
        String ipNormalizado = normalizarIp(ip);

        tentativasPorIp.compute(ipNormalizado, (chave, tentativaAtual) -> {
            Instant agora = Instant.now();

            if (tentativaAtual == null || tentativaAtual.expirouParaReset(agora)) {
                TentativaLogin novaTentativa = new TentativaLogin(ipNormalizado, agora);
                novaTentativa.incrementar();

                log.debug("ServicoTentativasLogin - Primeira falha registrada. ip={}, tentativas={}",
                        ipNormalizado, novaTentativa.getContador());

                return novaTentativa;
            }

            tentativaAtual.incrementar();
            tentativaAtual.setUltimoErro(agora);

            if (tentativaAtual.getContador() >= MAX_TENTATIVAS) {
                tentativaAtual.bloquearAte(agora.plusSeconds(TEMPO_BLOQUEIO_SEGUNDOS));
                log.warn("ServicoTentativasLogin - IP bloqueado por excesso de falhas. ip={}, tentativas={}, bloqueadoAte={}",
                        ipNormalizado, tentativaAtual.getContador(), tentativaAtual.getBloqueadoAte());
            } else {
                log.warn("ServicoTentativasLogin - Falha de login registrada. ip={}, tentativas={}",
                        ipNormalizado, tentativaAtual.getContador());
            }

            return tentativaAtual;
        });
    }

    public void resetarTentativas(String ip) {
        String ipNormalizado = normalizarIp(ip);
        tentativasPorIp.remove(ipNormalizado);
        log.debug("ServicoTentativasLogin - Tentativas resetadas. ip={}", ipNormalizado);
    }

    @Scheduled(fixedDelay = 300_000)
    public void limparAntigos() {
        Instant agora = Instant.now();
        int tamanhoAntes = tentativasPorIp.size();

        tentativasPorIp.entrySet().removeIf(entry -> entry.getValue().expirouParaReset(agora));

        int removidos = tamanhoAntes - tentativasPorIp.size();
        if (removidos > 0) {
            log.debug("ServicoTentativasLogin - Registros expirados removidos. total={}", removidos);
        }
    }

    public boolean estaBloqueado(String ip) {
        String ipNormalizado = normalizarIp(ip);
        TentativaLogin tentativa = tentativasPorIp.get(ipNormalizado);
        return tentativa != null && tentativa.estaBloqueado(Instant.now());
    }

    public int quantidadeTentativas(String ip) {
        String ipNormalizado = normalizarIp(ip);
        TentativaLogin tentativa = tentativasPorIp.get(ipNormalizado);
        return tentativa != null ? tentativa.getContador() : 0;
    }

    private String normalizarIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "desconhecido";
        }
        return ip.trim();
    }

    private static final class TentativaLogin {

        private final String ip;
        private int contador;
        private Instant ultimoErro;
        private Instant bloqueadoAte;

        private TentativaLogin(String ip, Instant ultimoErro) {
            this.ip = ip;
            this.ultimoErro = ultimoErro;
            this.contador = 0;
        }

        private void incrementar() {
            this.contador++;
        }

        private boolean estaBloqueado(Instant agora) {
            return bloqueadoAte != null && agora.isBefore(bloqueadoAte);
        }

        private boolean expirouParaReset(Instant agora) {
            return ultimoErro != null && agora.isAfter(ultimoErro.plusSeconds(TEMPO_RESET_SEGUNDOS));
        }

        private void bloquearAte(Instant instante) {
            this.bloqueadoAte = instante;
        }

        private int getContador() {
            return contador;
        }

        private Instant getBloqueadoAte() {
            return bloqueadoAte;
        }

        private void setUltimoErro(Instant ultimoErro) {
            this.ultimoErro = ultimoErro;
        }

        @SuppressWarnings("unused")
        private String getIp() {
            return ip;
        }
    }
}