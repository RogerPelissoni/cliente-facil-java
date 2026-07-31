package br.com.clientefacil.core.security;

import java.security.Principal;

// Identifica a sessão STOMP pelo userId (não pelo email), porque é assim que o resto do
// domínio já referencia usuários (Notification.userId, etc.) — evita ter que resolver
// email -> id toda vez que uma feature futura precisar mandar algo "para o usuário X".
public record StompPrincipal(String name) implements Principal {

    @Override
    public String getName() {
        return name;
    }
}
