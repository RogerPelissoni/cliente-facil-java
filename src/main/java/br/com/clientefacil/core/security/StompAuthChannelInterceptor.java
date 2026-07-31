package br.com.clientefacil.core.security;

import br.com.clientefacil.core.security.entity.AuthenticatedUser;
import br.com.clientefacil.core.service.AuthenticatedUserService;
import br.com.clientefacil.core.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * Equivalente do JwtFilter (que autentica requisições HTTP), só que para o handshake STOMP.
 * <p>
 * O upgrade HTTP inicial de WebSocket (GET /ws) não carrega o header Authorization — a API
 * nativa do navegador não permite enviar headers customizados nessa etapa, por isso /ws/**
 * é permitAll no SecurityConfig. Mas uma vez que a conexão WebSocket está aberta, o cliente
 * STOMP manda um frame CONNECT que É uma mensagem de aplicação comum, e essa sim pode carregar
 * headers arbitrários — inclusive um "Authorization: Bearer ...". É esse frame que autenticamos
 * aqui, exatamente como o JwtFilter faz para requisições REST.
 * <p>
 * Se o token for válido, associamos um StompPrincipal(userId) à sessão STOMP. É esse principal
 * que o Spring usa depois para rotear mensagens "para o usuário X" via
 * SimpMessagingTemplate.convertAndSendToUser(userId, destino, payload) — ver NotificationListener.
 * <p>
 * Se o token for inválido/ausente, a conexão segue sem principal (mesma filosofia do JwtFilter:
 * não autenticado, mas não derruba a conexão) — nesse caso a sessão simplesmente nunca recebe
 * nada endereçado a um usuário específico.
 */
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final AuthenticatedUserService authenticatedUserService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authorizationHeader = accessor.getFirstNativeHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return;
        }

        try {
            String token = authorizationHeader.substring(7).trim();
            String email = jwtService.extractEmail(token);

            if (email != null && jwtService.isTokenValid(token, email)) {
                AuthenticatedUser user = authenticatedUserService.loadByEmail(email);
                accessor.setUser(new StompPrincipal(String.valueOf(user.getUserId())));
            }
        } catch (Exception ignored) {
        }
    }
}
