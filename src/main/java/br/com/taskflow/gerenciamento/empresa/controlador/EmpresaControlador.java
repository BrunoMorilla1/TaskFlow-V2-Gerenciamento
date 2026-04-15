package br.com.taskflow.gerenciamento.empresa.controlador;

import br.com.taskflow.gerenciamento.empresa.cliente.BrasilApiCliente;
import br.com.taskflow.gerenciamento.empresa.dto.externo.BrasilApiCnpj;
import br.com.taskflow.gerenciamento.empresa.dto.requisicao.EmpresaAtualizacaoRequisicao;
import br.com.taskflow.gerenciamento.empresa.dto.requisicao.EmpresaCriacaoRequisicao;
import br.com.taskflow.gerenciamento.empresa.dto.resposta.EmpresaResponse;
import br.com.taskflow.gerenciamento.empresa.dto.resposta.EmpresaResumoResponse;
import br.com.taskflow.gerenciamento.empresa.servico.EmpresaServico;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/empresas")
@RequiredArgsConstructor
@Slf4j
public class EmpresaControlador {

    private final EmpresaServico empresaServico;
    private final BrasilApiCliente brasilApiClient;

    @PostMapping
    public ResponseEntity<EmpresaResponse> criarEmpresa(@Valid @RequestBody EmpresaCriacaoRequisicao requisicao,
                                                        @AuthenticationPrincipal UserDetails principal,
                                                        HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        log.info("Controlador[Empresa] - Requisição para criação de empresa. usuario={}, ip={}", principal.getUsername(), ip);

        EmpresaResponse response = empresaServico.criarEmpresa(requisicao, principal.getUsername(), ip);

        log.info("Controlador[Empresa] - Empresa criada com sucesso. empresaId={}, usuario={}, ip={}",
                response.id(), principal.getUsername(), ip);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/consultar-cnpj/{cnpj}")
    public ResponseEntity<BrasilApiCnpj> consultarDadosPublicos(@PathVariable String cnpj) {
        log.info("Controlador[Empresa] - Consulta pública de CNPJ solicitada: {}", cnpj);
        try {
            BrasilApiCnpj dados = brasilApiClient.buscarCnpj(cnpj.replaceAll("\\D", ""));
            return ResponseEntity.ok(dados);
        } catch (Exception e) {
            log.warn("Controlador[Empresa] - Não foi possível localizar dados para o CNPJ: {}", cnpj);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmpresaResponse> buscarPorId(@PathVariable Long id,
                                                       @AuthenticationPrincipal UserDetails principal,
                                                       HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        log.info("Controlador[Empresa] - Requisição para buscar empresa por id. empresaId={}, usuario={}, ip={}",
                id, principal.getUsername(), ip);

        EmpresaResponse response = empresaServico.buscarPorId(id, principal.getUsername(), ip);

        log.info("Controlador[Empresa] - Empresa encontrada com sucesso. empresaId={}, usuario={}, ip={}",
                response.id(), principal.getUsername(), ip);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<EmpresaResumoResponse>> listarMinhasEmpresas(@AuthenticationPrincipal UserDetails principal,
                                                                            HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        log.info("Controlador[Empresa] - Requisição para listar empresas do usuário. usuario={}, ip={}", principal.getUsername(), ip);

        List<EmpresaResumoResponse> response = empresaServico.listarMinhasEmpresas(principal.getUsername(), ip);

        log.info("Controlador[Empresa] - Listagem de empresas concluída. usuario={}, quantidade={}, ip={}",
                principal.getUsername(), response.size(), ip);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmpresaResponse> atualizarEmpresa(@PathVariable Long id,
                                                            @Valid @RequestBody EmpresaAtualizacaoRequisicao requisicao,
                                                            @AuthenticationPrincipal UserDetails principal,
                                                            HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        log.info("Controlador[Empresa] - Requisição para atualizar empresa. empresaId={}, usuario={}, ip={}",
                id, principal.getUsername(), ip);

        EmpresaResponse response = empresaServico.atualizarEmpresa(id, requisicao, principal.getUsername(), ip);

        log.info("Controlador[Empresa] - Empresa atualizada com sucesso. empresaId={}, usuario={}, ip={}",
                response.id(), principal.getUsername(), ip);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarEmpresa(@PathVariable Long id,
                                               @AuthenticationPrincipal UserDetails principal,
                                               HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        log.info("Controlador[Empresa] - Requisição para exclusão lógica de empresa. empresaId={}, usuario={}, ip={}",
                id, principal.getUsername(), ip);

        empresaServico.deletarEmpresa(id, principal.getUsername(), ip);

        log.info("Controlador[Empresa] - Empresa deletada com sucesso. empresaId={}, usuario={}, ip={}",
                id, principal.getUsername(), ip);

        return ResponseEntity.noContent().build();
    }
}