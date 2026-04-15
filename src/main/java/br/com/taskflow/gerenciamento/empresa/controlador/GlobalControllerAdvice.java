package br.com.taskflow.gerenciamento.empresa.controlador;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    @ModelAttribute("usuario")
    public UserDetails addUsuarioToModel(@AuthenticationPrincipal UserDetails principal) {
        return principal;
    }
}