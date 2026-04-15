package br.com.taskflow.gerenciamento.seguranca.autenticacao.dto.requisicao;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record LoginRequisicao(

        @NotBlank(message = "O email é obrigatório")
        @Email(message = "O email informado é inválido")
        @Size(max = 150, message = "O email deve ter no máximo 150 caracteres")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, max = 100, message = "A senha deve ter entre 8 e 100 caracteres")
        String senha

) {

        public String emailNormalizado() {
                return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        }

        public String senhaNormalizada() {
                return senha == null ? null : senha.trim();
        }

        public boolean possuiEmailValido() {
                return email != null && !email.isBlank();
        }

        public boolean possuiSenhaValida() {
                return senha != null && !senha.isBlank();
        }
}