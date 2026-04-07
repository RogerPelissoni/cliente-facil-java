package br.com.clientefacil.controller;

import br.com.clientefacil.core.CoreController;
import br.com.clientefacil.core.CoreService;
import br.com.clientefacil.dto.UserRequest;
import br.com.clientefacil.dto.UserResponse;
import br.com.clientefacil.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController extends CoreController<UserResponse, Long> {

    private final UserService service;

    @Override
    protected CoreService<?, UserResponse, Long> getService() {
        return service;
    }

    @PostMapping
    public UserResponse create(@RequestBody @Valid UserRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public UserResponse update(
            @PathVariable Long id,
            @RequestBody @Valid UserRequest request
    ) {
        return service.update(id, request);
    }
}