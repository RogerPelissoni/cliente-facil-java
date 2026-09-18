package br.com.clientefacil.repository;

import br.com.clientefacil.entity.EventService;
import br.com.clientefacil.entity.enums.EventStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface EventServiceRepository extends JpaRepository<EventService, Long>, JpaSpecificationExecutor<EventService> {

    // cancelledStatus por parâmetro, não literal — um literal gera cast pro nome da classe Java, que
    // não bate com o tipo do Postgres. cast(:excludeEventId as long) is null, não "is null or" puro —
    // mesma limitação do driver com parâmetro nulo (ver EventRepository#findForReport).
    @Query("""
                select case when count(es) > 0 then true else false end
                from EventService es
                join es.event e
                where es.professional.id = :professionalId
                  and e.tpStatus <> :cancelledStatus
                  and e.dtStart < :dtEnd
                  and e.dtEnd > :dtStart
                  and (cast(:excludeEventId as long) is null or e.id <> :excludeEventId)
            """)
    boolean existsOverlapping(
            @Param("professionalId") Long professionalId,
            @Param("dtStart") LocalDateTime dtStart,
            @Param("dtEnd") LocalDateTime dtEnd,
            @Param("excludeEventId") Long excludeEventId,
            @Param("cancelledStatus") EventStatusEnum cancelledStatus
    );
}
