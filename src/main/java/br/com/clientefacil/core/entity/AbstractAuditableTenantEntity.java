package br.com.clientefacil.core.entity;

import br.com.clientefacil.core.security.util.SecurityUtil;
import br.com.clientefacil.core.tenant.TenantIsolationContext;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.springframework.security.access.AccessDeniedException;

@MappedSuperclass
@Getter
@Setter
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "(company_id = :companyId OR company_id IS NULL)")
public abstract class AbstractAuditableTenantEntity extends AbstractAuditableEntity {

    @Column(name = "company_id")
    @JsonIgnore
    private Long companyId;

    @Transient
    @JsonIgnore
    private boolean globalScope;

    @SuppressWarnings("unused")
    public void markAsGlobalScope() {
        this.globalScope = true;
        this.companyId = null;
    }

    @PrePersist
    protected void assignTenantIfMissing() {
        if (!globalScope && companyId == null) {
            SecurityUtil.getAuthenticatedCompanyId().ifPresent(this::setCompanyId);
        }
    }

    @PostLoad
    protected void enforceTenantIsolation() {
        if (TenantIsolationContext.isBypassEnabled()) {
            return;
        }

        SecurityUtil.getAuthenticatedCompanyId().ifPresent(currentCompanyId -> {
            if (companyId != null && !companyId.equals(currentCompanyId)) {
                throw new AccessDeniedException("Tenant isolation violation");
            }
        });
    }
}
