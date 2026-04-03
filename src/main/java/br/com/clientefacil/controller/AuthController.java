package br.com.clientefacil.controller;

import br.com.clientefacil.security.JwtUtil;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password) {

        if ("admin".equals(username) && "123".equals(password)) {
            return JwtUtil.generateToken(username);
        }

        throw new RuntimeException("Credenciais inválidas");
    }
}
