package br.com.clientefacil.controller;

import br.com.clientefacil.dto.AuthRequest;
import br.com.clientefacil.dto.AuthResponse;
import br.com.clientefacil.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody @Valid AuthRequest request) {
        return service.login(request);
    }
}