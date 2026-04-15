package br.com.taskflow.gerenciamento.usuarios.dto.resposta;

import br.com.taskflow.gerenciamento.usuarios.enums.RoleUsuario;

import java.time.LocalDateTime;

public record UsuarioRespostaDTO(

        Long id,

        String nome,

        String email,

        RoleUsuario role,

        boolean ativo,

        int tentativasLogin,

        LocalDateTime ultimoLogin,

        LocalDateTime criadoEm

) {

    public boolean possuiTentativasDeLogin() {
        return tentativasLogin > 0;
    }

    public boolean jaRealizouLogin() {
        return ultimoLogin != null;
    }

    public boolean estaAtivo() {
        return ativo;
    }
}