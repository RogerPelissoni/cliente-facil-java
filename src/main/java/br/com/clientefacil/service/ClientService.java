package br.com.clientefacil.service;

import br.com.clientefacil.dto.ClientRequest;
import br.com.clientefacil.entity.Client;
import br.com.clientefacil.repository.ClientRepository;
import org.springframework.stereotype.Service;

@Service
public class ClientService {

    private final ClientRepository repository;

    public ClientService(ClientRepository repository) {
        this.repository = repository;
    }

    public Client findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
    }

    public Client create(ClientRequest request) {
        Client client = new Client();
        client.setName(request.name());
        client.setPhone(request.phone());
        client.setEmail(request.email());

        return repository.save(client);
    }
}