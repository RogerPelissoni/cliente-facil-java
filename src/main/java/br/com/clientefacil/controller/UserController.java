package br.com.clientefacil.controller;

import br.com.clientefacil.dto.UserRequest;
import br.com.clientefacil.entity.User;
import br.com.clientefacil.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public User findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public User create(@RequestBody @Valid UserRequest request) {
        return service.create(request);
    }
}