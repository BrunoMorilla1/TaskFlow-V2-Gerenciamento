package br.com.taskflow.gerenciamento.usuarios.servico;

import br.com.taskflow.gerenciamento.usuarios.dto.requisicao.UsuarioRequisicaoDTO;
import br.com.taskflow.gerenciamento.usuarios.dto.resposta.UsuarioRespostaDTO;
import br.com.taskflow.gerenciamento.usuarios.entidade.Usuario;
import br.com.taskflow.gerenciamento.excecao.EmailJaCadastradoException;
import br.com.taskflow.gerenciamento.excecao.UsuarioNaoEncontradoException;
import br.com.taskflow.gerenciamento.usuarios.mapper.UsuarioMapper;
import br.com.taskflow.gerenciamento.usuarios.repositorio.UsuarioRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioServico {

    private final UsuarioRepositorio usuarioRepositorio;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UsuarioRespostaDTO criarUsuario(UsuarioRequisicaoDTO dto) {
        validarDto(dto);

        final String emailNormalizado = normalizarEmail(dto.email());
        log.info("Servico[Usuario] - Iniciando criação de usuário. email={}", emailNormalizado);

        validarEmailDuplicado(emailNormalizado);

        Usuario usuario = usuarioMapper.paraEntidade(dto);
        usuario.setEmail(emailNormalizado);
        usuario.setSenha(passwordEncoder.encode(dto.senha()));

        Usuario usuarioSalvo = usuarioRepositorio.save(usuario);

        log.info("Servico[Usuario] - Usuário criado com sucesso. id={}, email={}",
                usuarioSalvo.getId(), usuarioSalvo.getEmail());

        return usuarioMapper.paraRespostaDTO(usuarioSalvo);
    }

    @Transactional(readOnly = true)
    public UsuarioRespostaDTO buscarPorId(Long id) {
        validarId(id);

        log.info("Servico[Usuario] - Buscando usuário por id={}", id);

        Usuario usuario = buscarEntidadePorId(id);

        log.debug("Servico[Usuario] - Usuário encontrado. id={}, email={}", usuario.getId(), usuario.getEmail());

        return usuarioMapper.paraRespostaDTO(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioRespostaDTO buscarPorEmail(String email) {
        final String emailNormalizado = normalizarEmailObrigatorio(email);

        log.info("Servico[Usuario] - Buscando usuário por email={}", emailNormalizado);

        Usuario usuario = usuarioRepositorio.findByEmailIgnoreCaseAndDeletadoFalse(emailNormalizado)
                .orElseThrow(() -> {
                    log.warn("Servico[Usuario] - Usuário não encontrado. email={}", emailNormalizado);
                    return new UsuarioNaoEncontradoException("Usuário não encontrado.");
                });

        return usuarioMapper.paraRespostaDTO(usuario);
    }

    @Transactional
    public UsuarioRespostaDTO atualizarUsuario(Long id, UsuarioRequisicaoDTO dto) {
        validarId(id);
        validarDto(dto);

        log.info("Servico[Usuario] - Iniciando atualização de usuário. id={}", id);

        Usuario usuario = buscarEntidadePorId(id);

        String emailAtualizado = normalizarEmail(dto.email());
        if (emailAtualizado != null && !emailAtualizado.equalsIgnoreCase(usuario.getEmail())) {
            validarEmailDuplicado(emailAtualizado);
        }

        usuarioMapper.atualizarEntidade(usuario, dto);

        Usuario usuarioAtualizado = usuarioRepositorio.save(usuario);

        log.info("Servico[Usuario] - Usuário atualizado com sucesso. id={}, email={}",
                usuarioAtualizado.getId(), usuarioAtualizado.getEmail());

        return usuarioMapper.paraRespostaDTO(usuarioAtualizado);
    }

    @Transactional
    public void deletarUsuario(Long id) {
        validarId(id);

        log.info("Servico[Usuario] - Iniciando exclusão lógica de usuário. id={}", id);

        Usuario usuario = buscarEntidadePorId(id);

        usuarioRepositorio.delete(usuario);

        log.info("Servico[Usuario] - Usuário deletado com sucesso. id={}, email={}",
                usuario.getId(), usuario.getEmail());
    }

    private Usuario buscarEntidadePorId(Long id) {
        return usuarioRepositorio.findByIdAndDeletadoFalse(id)
                .orElseThrow(() -> {
                    log.warn("Servico[Usuario] - Usuário não encontrado. id={}", id);
                    return new UsuarioNaoEncontradoException("Usuário não encontrado.");
                });
    }

    private void validarEmailDuplicado(String email) {
        if (usuarioRepositorio.existsByEmailIgnoreCaseAndDeletadoFalse(email)) {
            log.warn("Servico[Usuario] - Tentativa de uso de email já cadastrado. email={}", email);
            throw new EmailJaCadastradoException("Email já cadastrado.");
        }
    }

    private void validarDto(UsuarioRequisicaoDTO dto) {
        if (dto == null) {
            log.error("Servico[Usuario] - DTO de usuário não informado");
            throw new IllegalArgumentException("Os dados do usuário são obrigatórios.");
        }
    }

    private void validarId(Long id) {
        if (id == null || id <= 0) {
            log.error("Servico[Usuario] - ID inválido informado: {}", id);
            throw new IllegalArgumentException("O id do usuário é inválido.");
        }
    }

    private String normalizarEmailObrigatorio(String email) {
        String emailNormalizado = normalizarEmail(email);
        if (emailNormalizado == null || emailNormalizado.isBlank()) {
            log.error("Servico[Usuario] - Email não informado para consulta");
            throw new IllegalArgumentException("O email é obrigatório.");
        }
        return emailNormalizado;
    }

    private String normalizarEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}