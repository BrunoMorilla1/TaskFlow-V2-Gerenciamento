package br.com.taskflow.gerenciamento.empresa.cliente;

import br.com.taskflow.gerenciamento.empresa.dto.externo.BrasilApiCnpj;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "brasil-api", url = "https://brasilapi.com.br/api/cnpj/v1")
public interface BrasilApiCliente {

    @GetMapping("/{cnpj}")
    BrasilApiCnpj buscarCnpj(@PathVariable("cnpj") String cnpj);
}