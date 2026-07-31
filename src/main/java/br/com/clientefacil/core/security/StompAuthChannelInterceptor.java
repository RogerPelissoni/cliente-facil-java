package br.com.clientefacil.core.security;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * Autentica o handshake STOMP (frame CONNECT) — equivalente ao JwtFilter, mas para WebSocket.
 * <p>
 * O upgrade HTTP inicial de WebSocket (GET /ws) não carrega o header Authorization — a API
 * nativa do navegador não permite enviar headers customizados nessa etapa, por isso /ws/**
 * é permitAll no SecurityConfig. Mas uma vez que a conexão WebSocket está aberta, o cliente
 * STOMP manda um frame CONNECT que É uma mensagem de aplicação comum, e essa sim pode carregar
 * headers arbitrários.
 * <p>
 * Diferente do JwtFilter, aqui não validamos o JWT completo: o header Authorization carrega um
 * "ws-ticket" — um token efêmero, de uso único, emitido por GET /api/v1/auth/ws-ticket (que esse
 * sim exige o JWT completo para ser gerado). Isso evita que o JWT de 24h precise ser exposto ao
 * JS do navegador só para autenticar essa conexão — ver WsTicketService.
 * <p>
 * Se o ticket for válido, associamos um StompPrincipal(userId) à sessão STOMP. É esse principal
 * que o Spring usa depois para rotear mensagens "para o usuário X" via
 * SimpMessagingTemplate.convertAndSendToUser(userId, destino, payload) — ver NotificationListener.
 * <p>
 * Se o ticket for inválido/ausente/expirado, a conexão segue sem principal (mesma filosofia do
 * JwtFilter: não autenticado, mas não derruba a conexão) — a sessão simplesmente nunca recebe
 * nada endereçado a um usuário específico.
 */
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final WsTicketService wsTicketService;

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

        String ticket = authorizationHeader.substring(7).trim();

        wsTicketService.consume(ticket)
                .ifPresent(userId -> accessor.setUser(new StompPrincipal(String.valueOf(userId))));
    }
}
