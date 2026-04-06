package br.com.clientefacil.controller;

import br.com.clientefacil.dto.UserRequest;
import br.com.clientefacil.dto.UserResponse;
import br.com.clientefacil.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    @GetMapping
    public List<UserResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable Long id) {
        return service.findById(id);
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

    @DeleteMapping("/{id}")
    @ResponseStatus(code = org.springframework.http.HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}