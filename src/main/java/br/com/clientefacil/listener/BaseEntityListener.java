package br.com.clientefacil.listener;

import br.com.clientefacil.entity.Auditable;
import br.com.clientefacil.entity.Tenant;
import br.com.clientefacil.entity.User;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;

public class BaseEntityListener {

    @PrePersist
    public void prePersist(Object entity) {

        // ===== AUDIT =====
        if (entity instanceof HasAuditable auditableEntity) {
            Auditable auditable = auditableEntity.getAuditable();

            if (auditable != null) {
                LocalDateTime now = LocalDateTime.now();

                if (auditable.getCreatedAt() == null) {
                    auditable.setCreatedAt(now);
                }

                if (auditable.getUpdatedAt() == null) {
                    auditable.setUpdatedAt(now);
                }

                User user = getCurrentUser();
                if (user != null) {
                    auditable.setCreatedBy(user);
                    auditable.setUpdatedBy(user);
                }
            }
        }

        // ===== TENANT =====
        if (entity instanceof HasTenant tenantEntity) {
            Tenant tenant = tenantEntity.getTenant();

            if (tenant != null && tenant.getCompany() == null) {
                User user = getCurrentUser();
                if (user != null && user.getTenant() != null) {
                    tenant.setCompany(user.getTenant().getCompany());
                }
            }
        }
    }

    @PreUpdate
    public void preUpdate(Object entity) {

        if (entity instanceof HasAuditable auditableEntity) {
            Auditable auditable = auditableEntity.getAuditable();

            if (auditable != null) {
                auditable.setUpdatedAt(LocalDateTime.now());

                User user = getCurrentUser();
                if (user != null) {
                    auditable.setUpdatedBy(user);
                }
            }
        }
    }

    private User getCurrentUser() {
        var context = SecurityContextHolder.getContext();

        if (context != null && context.getAuthentication() != null) {
            Object principal = context.getAuthentication().getPrincipal();

            if (principal instanceof User user) {
                return user;
            }
        }

        return null;
    }
}