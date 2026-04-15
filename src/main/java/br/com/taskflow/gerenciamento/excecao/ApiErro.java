package br.com.taskflow.gerenciamento.excecao;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErro(
        int status,
        String erro,
        String codigo,
        String mensagem,
        String path,
        LocalDateTime timestamp,
        Map<String, String> erros
) {

    public static ApiErro of(int status, String erro, String codigo, String mensagem, String path) {
        return new ApiErro(
                status,
                erro,
                codigo,
                mensagem,
                path,
                LocalDateTime.now(),
                null
        );
    }

    public static ApiErro comValidacao(
            int status,
            String erro,
            String codigo,
            String mensagem,
            String path,
            Map<String, String> erros
    ) {
        return new ApiErro(
                status,
                erro,
                codigo,
                mensagem,
                path,
                LocalDateTime.now(),
                erros
        );
    }
}