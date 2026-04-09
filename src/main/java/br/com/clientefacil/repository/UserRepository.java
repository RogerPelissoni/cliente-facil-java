package br.com.clientefacil.repository;

import br.com.clientefacil.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {
            "profile",
            "profile.permissions",
            "profile.permissions.resource"
    })
    Optional<User> findByEmail(String email);
}