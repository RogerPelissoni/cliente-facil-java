package br.com.clientefacil.entity.base;

import br.com.clientefacil.security.SecurityUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

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

    public void markAsGlobalScope() {
        this.globalScope = true;
        this.companyId = null;
    }

    @PrePersist
    protected void assignTenantIfMissing() {
        if (!globalScope && companyId == null) {
            SecurityUtils.getCurrentCompanyId().ifPresent(this::setCompanyId);
        }
    }
}
