package br.com.taskflow.gerenciamento.seguranca.rateLimit;

import br.com.taskflow.gerenciamento.excecao.RateLimitException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ServicoRateLimit {

    private static final int LIMITE_REQUISICOES_POR_JANELA = 100;
    private static final int JANELA_EM_SEGUNDOS = 60;
    private static final int TEMPO_EXPIRACAO_BUCKET_EM_SEGUNDOS = JANELA_EM_SEGUNDOS * 2;

    private final Map<String, Bucket> bucketsPorIp = new ConcurrentHashMap<>();

    public void validarLimite(String ip) {
        String ipNormalizado = normalizarIp(ip);

        Bucket bucket = bucketsPorIp.computeIfAbsent(ipNormalizado, this::criarBucket);

        synchronized (bucket) {
            Instant agora = Instant.now();

            if (bucket.janelaExpirada(agora, JANELA_EM_SEGUNDOS)) {
                bucket.resetar(agora);
            }

            bucket.incrementar();

            if (bucket.getRequisicoes() > LIMITE_REQUISICOES_POR_JANELA) {
                log.warn("ServicoRateLimit - Limite excedido. ip={}, requisicoes={}, limite={}",
                        ipNormalizado, bucket.getRequisicoes(), LIMITE_REQUISICOES_POR_JANELA);

                throw new RateLimitException("Limite de requisições excedido.");
            }
        }
    }

    @Scheduled(fixedDelay = 300_000)
    public void limparBucketsAntigos() {
        Instant agora = Instant.now();
        int tamanhoAntes = bucketsPorIp.size();

        bucketsPorIp.entrySet().removeIf(entry ->
                entry.getValue().expiradoParaLimpeza(agora, TEMPO_EXPIRACAO_BUCKET_EM_SEGUNDOS)
        );

        int removidos = tamanhoAntes - bucketsPorIp.size();

        if (removidos > 0) {
            log.debug("ServicoRateLimit - Buckets expirados removidos. total={}", removidos);
        }
    }

    public int obterQuantidadeBuckets() {
        return bucketsPorIp.size();
    }

    private Bucket criarBucket(String ip) {
        return new Bucket(ip, Instant.now());
    }

    private String normalizarIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "desconhecido";
        }
        return ip.trim();
    }

    private static final class Bucket {

        private final String ip;
        private int requisicoes;
        private Instant inicioJanela;

        private Bucket(String ip, Instant inicioJanela) {
            this.ip = ip;
            this.inicioJanela = inicioJanela;
            this.requisicoes = 0;
        }

        private void incrementar() {
            this.requisicoes++;
        }

        private void resetar(Instant agora) {
            this.requisicoes = 0;
            this.inicioJanela = agora;
        }

        private boolean janelaExpirada(Instant agora, int janelaEmSegundos) {
            return agora.isAfter(inicioJanela.plusSeconds(janelaEmSegundos));
        }

        private boolean expiradoParaLimpeza(Instant agora, int tempoExpiracaoEmSegundos) {
            return agora.isAfter(inicioJanela.plusSeconds(tempoExpiracaoEmSegundos));
        }

        private int getRequisicoes() {
            return requisicoes;
        }

        @SuppressWarnings("unused")
        private String getIp() {
            return ip;
        }
    }
}