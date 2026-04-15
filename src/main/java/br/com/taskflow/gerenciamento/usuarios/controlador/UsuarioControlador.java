package br.com.taskflow.gerenciamento.usuarios.controlador;

import br.com.taskflow.gerenciamento.usuarios.dto.requisicao.UsuarioRequisicaoDTO;
import br.com.taskflow.gerenciamento.usuarios.dto.resposta.UsuarioRespostaDTO;
import br.com.taskflow.gerenciamento.usuarios.servico.UsuarioServico;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@Slf4j
public class UsuarioControlador {

    private final UsuarioServico usuarioServico;

    @PostMapping
    public ResponseEntity<UsuarioRespostaDTO> criarUsuario(@Valid @RequestBody UsuarioRequisicaoDTO dto) {
        log.info("Controller[Usuario] - Recebida requisição para criação de usuário. email={}", dto.emailNormalizado());

        UsuarioRespostaDTO resposta = usuarioServico.criarUsuario(dto);

        log.info("Controller[Usuario] - Usuário criado com sucesso. id={}", resposta.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioRespostaDTO> buscarPorId(@PathVariable Long id) {
        log.info("Controller[Usuario] - Recebida requisição de busca por id. id={}", id);

        UsuarioRespostaDTO resposta = usuarioServico.buscarPorId(id);

        log.debug("Controller[Usuario] - Busca por id concluída com sucesso. id={}", id);
        return ResponseEntity.ok(resposta);
    }

    @GetMapping("/email")
    public ResponseEntity<UsuarioRespostaDTO> buscarPorEmail(@RequestParam String email) {
        log.info("Controller[Usuario] - Recebida requisição de busca por email. email={}", email);

        UsuarioRespostaDTO resposta = usuarioServico.buscarPorEmail(email);

        log.debug("Controller[Usuario] - Busca por email concluída com sucesso. email={}", email);
        return ResponseEntity.ok(resposta);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioRespostaDTO> atualizarUsuario(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioRequisicaoDTO dto
    ) {
        log.info("Controller[Usuario] - Recebida requisição para atualização de usuário. id={}", id);

        UsuarioRespostaDTO resposta = usuarioServico.atualizarUsuario(id, dto);

        log.info("Controller[Usuario] - Usuário atualizado com sucesso. id={}", id);
        return ResponseEntity.ok(resposta);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarUsuario(@PathVariable Long id) {
        log.info("Controller[Usuario] - Recebida requisição para exclusão lógica de usuário. id={}", id);

        usuarioServico.deletarUsuario(id);

        log.info("Controller[Usuario] - Usuário deletado com sucesso. id={}", id);
        return ResponseEntity.noContent().build();
    }
}