package br.com.taskflow.gerenciamento.seguranca.autenticacao.dto.requisicao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshTokenRequisicao(

        @NotBlank(message = "O refresh token é obrigatório")
        @Size(max = 2000, message = "O refresh token excede o tamanho permitido")
        String refreshToken

) {

        public String tokenNormalizado() {
                return refreshToken == null ? null : refreshToken.trim();
        }

        public boolean possuiTokenValido() {
                return refreshToken != null && !refreshToken.isBlank();
        }
}