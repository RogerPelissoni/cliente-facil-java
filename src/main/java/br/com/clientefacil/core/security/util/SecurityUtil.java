package br.com.clientefacil.core.security.util;

import br.com.clientefacil.core.security.entity.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityUtil {

    private SecurityUtil() {
    }

    public static Optional<AuthenticatedUser> getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser authenticatedUser) {
            return Optional.of(authenticatedUser);
        }

        return Optional.empty();
    }

    public static Optional<Long> getAuthenticatedUserId() {
        return getAuthenticatedUser().map(AuthenticatedUser::getUserId);
    }

    public static Optional<Long> getAuthenticatedCompanyId() {
        return getAuthenticatedUser().map(AuthenticatedUser::getCompanyId);
    }
}
