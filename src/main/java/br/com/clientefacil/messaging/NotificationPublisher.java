package br.com.clientefacil.messaging;

import br.com.clientefacil.core.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(NotificationMessageDTO message) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.NOTIFICATION_EXCHANGE, RabbitMQConfig.NOTIFICATION_ROUTING_KEY, message);
    }
}
