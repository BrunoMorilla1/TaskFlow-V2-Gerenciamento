package br.com.taskflow.gerenciamento;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LandingPageController {

    @GetMapping("/")
    public String index() {
        return "landing";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/empresa")
    public String empresa() {
        return "empresa";
    }

    @GetMapping("/planos")
    public String planos() {
        return "planos";
    }

    @GetMapping("/sucesso")
    public String sucesso() {
        return "sucesso";
    }
}
