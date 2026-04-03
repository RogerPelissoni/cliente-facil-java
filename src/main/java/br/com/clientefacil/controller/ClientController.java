package br.com.clientefacil.controller;

import br.com.clientefacil.dto.ClientRequest;
import br.com.clientefacil.entity.Client;
import br.com.clientefacil.service.ClientService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/client")
public class ClientController {

    private final ClientService service;

    public ClientController(ClientService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public Client findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public Client create(@RequestBody @Valid ClientRequest request) {
        return service.create(request);
    }
}