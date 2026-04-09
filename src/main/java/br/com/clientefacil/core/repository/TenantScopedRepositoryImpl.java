package br.com.clientefacil.core.repository;

import br.com.clientefacil.core.entity.AbstractAuditableTenantEntity;
import br.com.clientefacil.core.security.util.SecurityUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.lang.NonNull;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public class TenantScopedRepositoryImpl<T, ID extends Serializable>
        extends SimpleJpaRepository<T, ID> {

    private final JpaEntityInformation<T, ?> entityInformation;
    private final EntityManager entityManager;

    public TenantScopedRepositoryImpl(
            JpaEntityInformation<T, ?> entityInformation,
            EntityManager entityManager
    ) {
        super(entityInformation, entityManager);
        this.entityInformation = entityInformation;
        this.entityManager = entityManager;
    }

    @Override
    @NonNull
    public Optional<T> findById(@NonNull ID id) {
        Class<T> domainClass = entityInformation.getJavaType();

        if (!AbstractAuditableTenantEntity.class.isAssignableFrom(domainClass)) {
            return super.findById(id);
        }

        if (SecurityUtil.getAuthenticatedUser().isEmpty()) {
            return super.findById(id);
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> cq = cb.createQuery(domainClass);
        Root<T> root = cq.from(domainClass);

        String idAttributeName = entityInformation.getRequiredIdAttribute().getName();
        cq.select(root).where(cb.equal(root.get(idAttributeName), id));

        TypedQuery<T> query = entityManager.createQuery(cq);
        List<T> results = query.getResultList();
        return results.stream().findFirst();
    }
}
