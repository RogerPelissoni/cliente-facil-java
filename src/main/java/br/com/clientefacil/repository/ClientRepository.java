package br.com.clientefacil.repository;

import br.com.clientefacil.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {
}