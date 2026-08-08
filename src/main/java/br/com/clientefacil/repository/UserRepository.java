package br.com.clientefacil.repository;

import br.com.clientefacil.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    @EntityGraph(attributePaths = {
            "profile",
            "profile.permissions",
            "profile.permissions.resource"
    })
    Optional<User> findByEmail(String email);

    @Query("""
                    select u.id, u.name
                    from User u
                    order by u.name
            """)
    List<Object[]> keyValue();

    @EntityGraph(attributePaths = {
            "person",
            "profile",
            "company"
    })
    @Query("select u from User u")
    Page<User> findAllWithRelations(Pageable pageable);

    @Query("""
                    select distinct u.id
                    from User u
                    join u.profile p
                    join p.permissions pp
                    join pp.resource r
                    where r.signature = :signature
            """)
    List<Long> findUserIdsByResourceSignature(String signature);

    // id + email juntos (em vez de duas queries separadas) pra evitar o risco de "zipar" duas
    // listas independentes por índice, que quebraria silenciosamente se a ordem divergisse.
    @Query("""
                    select distinct u.id, u.email
                    from User u
                    join u.profile p
                    join p.permissions pp
                    join pp.resource r
                    where r.signature = :signature
            """)
    List<Object[]> findIdAndEmailByResourceSignature(String signature);
}