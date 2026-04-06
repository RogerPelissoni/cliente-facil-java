package br.com.clientefacil.controller;

import br.com.clientefacil.dto.AuthRequest;
import br.com.clientefacil.dto.AuthResponse;
import br.com.clientefacil.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    @PostMapping("/login")
    public AuthResponse login(@RequestBody @Valid AuthRequest request) {
        return service.login(request);
    }
}