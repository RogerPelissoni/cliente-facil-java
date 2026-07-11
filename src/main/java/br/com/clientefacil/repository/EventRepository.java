package br.com.clientefacil.repository;

import br.com.clientefacil.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

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
}
