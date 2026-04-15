package br.com.taskflow.gerenciamento.usuarios.dto.requisicao;

import br.com.taskflow.gerenciamento.usuarios.enums.OrigemCadastro;
import br.com.taskflow.gerenciamento.usuarios.enums.RoleUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UsuarioRequisicaoDTO(

        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 3, max = 150, message = "O nome deve ter entre 3 e 150 caracteres")
        String nome,

        @NotBlank(message = "O email é obrigatório")
        @Email(message = "O email informado é inválido")
        @Size(max = 150, message = "O email deve ter no máximo 150 caracteres")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, max = 100, message = "A senha deve ter entre 8 e 100 caracteres")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "A senha deve conter pelo menos uma letra e um número"
        )
        String senha,

        RoleUsuario role,

        OrigemCadastro origemCadastro

) {

        public String emailNormalizado() {
                return email == null ? null : email.trim().toLowerCase();
        }

        public String nomeNormalizado() {
                return nome == null ? null : nome.trim();
        }

        public boolean possuiRole() {
                return role != null;
        }

        public boolean possuiOrigemCadastro() {
                return origemCadastro != null;
        }
}