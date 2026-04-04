package br.com.clientefacil.security;

import org.springframework.data.domain.AuditorAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("currentUserAuditorAware")
public class CurrentUserAuditorAware implements AuditorAware<Long> {

    @Override
    @NonNull
    public Optional<Long> getCurrentAuditor() {
        return SecurityUtils.getCurrentUserId();
    }
}
