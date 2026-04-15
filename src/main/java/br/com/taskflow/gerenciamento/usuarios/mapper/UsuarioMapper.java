package br.com.taskflow.gerenciamento.usuarios.mapper;

import br.com.taskflow.gerenciamento.usuarios.dto.requisicao.UsuarioRequisicaoDTO;
import br.com.taskflow.gerenciamento.usuarios.dto.resposta.UsuarioRespostaDTO;
import br.com.taskflow.gerenciamento.usuarios.entidade.Usuario;
import br.com.taskflow.gerenciamento.usuarios.enums.RoleUsuario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@Slf4j
public class UsuarioMapper {

    public Usuario paraEntidade(UsuarioRequisicaoDTO dto) {
        validarDto(dto);

        final String emailNormalizado = normalizarEmail(dto.email());
        final String nomeNormalizado = normalizarTexto(dto.nome());
        final RoleUsuario role = dto.role() != null ? dto.role() : RoleUsuario.USER;

        log.debug("Mapper[Usuario] - Convertendo DTO para entidade. email={}, role={}", emailNormalizado, role);

        return Usuario.builder()
                .nome(nomeNormalizado)
                .email(emailNormalizado)
                .senha(dto.senha())
                .role(role)
                .ativo(true)
                .contaNaoBloqueada(true)
                .contaNaoExpirada(true)
                .credencialNaoExpirada(true)
                .habilitado(true)
                .tentativasLogin(0)
                .deletado(false)
                .build();
    }

    public void atualizarEntidade(Usuario usuario, UsuarioRequisicaoDTO dto) {
        validarUsuario(usuario);
        validarDto(dto);

        log.debug("Mapper[Usuario] - Atualizando entidade usuarioId={}", usuario.getId());

        if (textoPreenchido(dto.nome())) {
            usuario.setNome(normalizarTexto(dto.nome()));
        }

        if (textoPreenchido(dto.email())) {
            usuario.setEmail(normalizarEmail(dto.email()));
        }

        if (dto.role() != null) {
            usuario.setRole(dto.role());
        }

    }

    public UsuarioRespostaDTO paraRespostaDTO(Usuario usuario) {
        validarUsuario(usuario);

        log.debug("Mapper[Usuario] - Convertendo entidade para resposta. usuarioId={}", usuario.getId());

        return new UsuarioRespostaDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getRole(),
                usuario.isAtivo(),
                usuario.getTentativasLogin(),
                usuario.getUltimoLogin(),
                usuario.getCriadoEm()
        );
    }

    private void validarDto(UsuarioRequisicaoDTO dto) {
        if (dto == null) {
            log.error("Mapper[Usuario] - DTO de requisição é nulo");
            throw new IllegalArgumentException("Os dados do usuário são obrigatórios.");
        }
    }

    private void validarUsuario(Usuario usuario) {
        if (usuario == null) {
            log.error("Mapper[Usuario] - Entidade Usuario é nula");
            throw new IllegalArgumentException("A entidade de usuário é obrigatória.");
        }
    }

    private boolean textoPreenchido(String valor) {
        return valor != null && !valor.isBlank();
    }

    private String normalizarEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizarTexto(String valor) {
        if (valor == null) {
            return null;
        }
        return valor.trim();
    }
}