package br.com.clientefacil.repository;

import br.com.clientefacil.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {
    @Query("""
                    select e.id, e.dsTitle
                    from Event e
                    order by e.dsTitle
            """)
    List<Object[]> keyValue();

    @Query("""
                select e
                from Event e
                join EventOwner eo
                    ON eo.event.id = e.id
                    AND eo.user.id = :userId
            """)
    List<Event> findAllByUser(Long userId);

    @Query("""
                select e
                from Event e
                join EventOwner eo
                    ON eo.event.id = e.id
                    AND eo.user.id = :userId
                where e.id = :id
            """)
    Optional<Event> findById(@NonNull Long id, Long userId);
}
