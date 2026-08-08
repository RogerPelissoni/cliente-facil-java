package br.com.clientefacil.messaging;

import br.com.clientefacil.core.config.EmailRabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

// Producer: fire-and-forget, mesmo padrão de NotificationPublisher. Reaproveita o RabbitTemplate
// já configurado (Jackson2JsonMessageConverter) em RabbitMQConfig.
@Service
@RequiredArgsConstructor
public class EmailPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(EmailMessageDTO message) {
        rabbitTemplate.convertAndSend(EmailRabbitMQConfig.EMAIL_EXCHANGE, EmailRabbitMQConfig.EMAIL_ROUTING_KEY, message);
    }
}
