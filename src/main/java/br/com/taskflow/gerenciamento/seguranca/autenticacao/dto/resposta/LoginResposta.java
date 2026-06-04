package br.com.taskflow.gerenciamento.seguranca.autenticacao.dto.resposta;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResposta(

        String accessToken,
        String refreshToken,
        String tipoToken,
        Long expiresIn,
        Instant emitidoEm

) {

    public static LoginResposta of(
            String accessToken,
            String refreshToken,
            Long expiresIn
    ) {
        return new LoginResposta(
                accessToken,
                refreshToken,
                "Bearer",
                expiresIn,
                Instant.now()
        );
    }

    public static LoginResposta semRefresh(
            String accessToken,
            Long expiresIn
    ) {
        return new LoginResposta(
                accessToken,
                null,
                "Bearer",
                expiresIn,
                Instant.now()
        );
    }

    public boolean possuiRefreshToken() {
        return refreshToken != null && !refreshToken.isBlank();
    }

    public boolean possuiAccessToken() {
        return accessToken != null && !accessToken.isBlank();
    }

}