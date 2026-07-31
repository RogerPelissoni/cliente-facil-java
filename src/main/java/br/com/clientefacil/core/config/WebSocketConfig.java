package br.com.clientefacil.core.config;

import br.com.clientefacil.core.security.StompAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Infraestrutura de tempo real GENÉRICA do projeto — não é específica de notificações.
 * Qualquer feature futura que precise "avisar um usuário quando algo termina de processar
 * no backend" (ex: exportação de PDF, geração de relatório) reaproveita isso sem tocar em
 * nada aqui: só publica em um novo destino via SimpMessagingTemplate.convertAndSendToUser(...)
 * e o front-end assina esse destino (ver NotificationListener como exemplo de uso).
 * <p>
 * Por que STOMP em vez de WebSocket "cru": STOMP multiplexa UMA única conexão WebSocket por
 * navegador em vários "destinos" lógicos (/user/queue/notifications, /user/queue/pdf-export,
 * etc). Sem isso, cada feature nova exigiria seu próprio endpoint WebSocket e sua própria
 * gestão de sessões (é o que fizemos na primeira versão deste recurso, só para notificações,
 * e não escalaria bem para múltiplas features).
 * <p>
 * - registerStompEndpoints: onde o cliente conecta (handshake). Um único endpoint "/ws" serve
 * todas as features.
 * - configureMessageBroker: habilita um broker em memória para os prefixos "/queue" (mensagens
 * para um destinatário específico) e "/topic" (broadcast, não usado ainda). O prefixo "/app"
 * seria para mensagens que o CLIENTE envia ao servidor (não usado aqui: este projeto só empurra
 * dados do servidor para o cliente, nunca o contrário).
 * - configureClientInboundChannel: registra o StompAuthChannelInterceptor, que autentica o
 * frame CONNECT via JWT (ver a própria classe para detalhes).
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue", "/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
