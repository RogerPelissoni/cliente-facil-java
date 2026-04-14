package br.com.clientefacil.repository;

import br.com.clientefacil.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    @EntityGraph(attributePaths = {
            "profile",
            "profile.permissions",
            "profile.permissions.resource"
    })
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = {
            "person",
            "profile",
            "company"
    })
    @Query("select u from User u")
    Page<User> findAllWithRelations(Pageable pageable);
}