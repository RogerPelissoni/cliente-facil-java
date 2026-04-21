package br.com.clientefacil.repository;

import br.com.clientefacil.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long>, JpaSpecificationExecutor<Profile> {
    @Query("""
                    select p.id, p.name
                    from Profile p
                    order by p.name
            """)
    List<Object[]> keyValue();

    Optional<Profile> findByName(String name);
}
