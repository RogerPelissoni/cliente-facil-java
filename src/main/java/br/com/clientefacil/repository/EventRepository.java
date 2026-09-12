package br.com.clientefacil.repository;

import br.com.clientefacil.entity.Event;
import br.com.clientefacil.entity.enums.EventStatusEnum;
import br.com.clientefacil.entity.enums.EventTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;
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
                join EventOwner eo on eo.event = e
                left join fetch e.eventService s
                left join fetch s.client
                left join fetch s.professional
                left join fetch s.accountReceivable
                where e.id = :id
                  and eo.user.id = :userId
            """)
    Optional<Event> findById(@NonNull Long id, @NonNull Long userId);

    // Sem paginação: o resumo precisa do conjunto inteiro (ver buildReportSummary).
    // coalesce em vez de "is null or" em dtStart/dtEnd/tpStatus/tpEvent (NOT NULL) — "is null or"
    // quebra no Postgres por erro de inferência de tipo. client/professional/ownerId vêm de left
    // join e podem ser null de verdade, por isso ali usam "is null or" + cast.
    // left join + distinct em EventOwner: sem garantia de schema de owner único por evento.
    @Query("""
                select distinct e
                from Event e
                left join fetch e.eventService es
                left join fetch es.client c
                left join fetch c.person cp
                left join fetch es.professional p
                left join fetch p.person pp
                left join fetch es.accountReceivable ar
                left join EventOwner eo on eo.event = e
                where e.dtStart >= coalesce(:dtStart, e.dtStart)
                  and e.dtStart <= coalesce(:dtEnd, e.dtStart)
                  and e.tpStatus = coalesce(:tpStatus, e.tpStatus)
                  and e.tpEvent = coalesce(:tpEvent, e.tpEvent)
                  and (cast(:clientId as long) is null or c.id = :clientId)
                  and (cast(:professionalId as long) is null or p.id = :professionalId)
                  and (cast(:ownerId as long) is null or eo.user.id = :ownerId)
                order by e.dtStart desc
            """)
    List<Event> findForReport(
            @Param("dtStart") LocalDateTime dtStart,
            @Param("dtEnd") LocalDateTime dtEnd,
            @Param("tpStatus") EventStatusEnum tpStatus,
            @Param("tpEvent") EventTypeEnum tpEvent,
            @Param("clientId") Long clientId,
            @Param("professionalId") Long professionalId,
            @Param("ownerId") Long ownerId
    );

    // À parte porque EventOwner não tem associação mapeada em Event, só o caminho inverso.
    @Query("""
                select eo.event.id, eo.user.id, eo.user.name
                from EventOwner eo
                where eo.event.id in :eventIds
            """)
    List<Object[]> findOwnersByEventIds(@Param("eventIds") List<Long> eventIds);
}
