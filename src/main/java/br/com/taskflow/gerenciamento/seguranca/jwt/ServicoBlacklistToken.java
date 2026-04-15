package br.com.taskflow.gerenciamento.seguranca.jwt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ServicoBlacklistToken {

    private static final int LIMITE_MAXIMO_TOKENS = 100_000;

    private final Map<String, Instant> tokensRevogados = new ConcurrentHashMap<>();

    public void revogarToken(String tokenId, Instant expiracao) {

        if (tokenId == null || tokenId.isBlank()) {
            log.warn("BlacklistToken - Tentativa de revogar token inválido (jti nulo/vazio)");
            return;
        }

        if (expiracao == null) {
            log.warn("BlacklistToken - Token sem expiração ignorado. jti={}", tokenId);
            return;
        }

        if (tokensRevogados.size() >= LIMITE_MAXIMO_TOKENS) {
            log.error("BlacklistToken - Limite máximo atingido. tokens={}", tokensRevogados.size());
            return;
        }

        tokensRevogados.put(tokenId, expiracao);

        log.debug("BlacklistToken - Token revogado. jti={} exp={}", tokenId, expiracao);
    }

    public boolean tokenRevogado(String tokenId) {

        if (tokenId == null || tokenId.isBlank()) {
            return false;
        }

        Instant expiracao = tokensRevogados.get(tokenId);

        if (expiracao == null) {
            return false;
        }

        if (Instant.now().isAfter(expiracao)) {

            tokensRevogados.remove(tokenId);

            log.debug("BlacklistToken - Token expirado removido. jti={}", tokenId);

            return false;
        }

        return true;
    }

    /**
     * Limpeza automática a cada 10 minutos
     */
    @Scheduled(fixedDelay = 600_000)
    public void limparTokensExpirados() {

        Instant agora = Instant.now();

        int tamanhoAntes = tokensRevogados.size();

        tokensRevogados.entrySet()
                .removeIf(entry -> agora.isAfter(entry.getValue()));

        int removidos = tamanhoAntes - tokensRevogados.size();

        if (removidos > 0) {
            log.debug("BlacklistToken - Tokens expirados removidos: {}", removidos);
        }
    }

    public int tamanhoAtual() {
        return tokensRevogados.size();
    }
}