package br.com.clientefacil.core.config;

import br.com.clientefacil.core.hibernate.ForeignKeyDeletionGuard;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.springframework.stereotype.Component;

// Sem precedente de listener custom no projeto até aqui — registro manual via EventListenerRegistry
// é o jeito padrão de plugar um listener no Hibernate a partir de um bean Spring (não existe um jeito
// declarativo/anotação pra isso). Ver ForeignKeyDeletionGuard pro que o listener faz.
@Component
@RequiredArgsConstructor
public class HibernateListenerConfig {

    private final EntityManagerFactory entityManagerFactory;
    private final ForeignKeyDeletionGuard foreignKeyDeletionGuard;

    @PostConstruct
    void registerListeners() {
        SessionFactoryImplementor sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor.class);
        EventListenerRegistry registry = sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);
        registry.appendListeners(EventType.PRE_DELETE, foreignKeyDeletionGuard);
    }
}
