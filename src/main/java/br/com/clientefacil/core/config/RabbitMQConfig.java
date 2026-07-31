package br.com.clientefacil.core.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Conceitos básicos do RabbitMQ usados aqui:
 * <p>
 * - Producer: quem envia a mensagem (não conhece a Queue, só o Exchange) — ver NotificationPublisher.
 * - Exchange: recebe a mensagem do producer e decide para qual(is) Queue(s) encaminhar.
 * Aqui usamos uma DirectExchange: ela encaminha a mensagem para a fila cuja
 * "routing key" seja EXATAMENTE igual à routing key usada no envio.
 * - Queue: onde a mensagem fica guardada até um Consumer processá-la.
 * - Binding: a "ligação" entre Exchange e Queue, associada a uma routing key.
 * É o que diz ao Exchange "mensagens com essa routing key vão para essa fila".
 * - Consumer: quem lê e processa a mensagem da Queue — ver NotificationListener.
 * <p>
 * Fluxo completo: Producer -> Exchange -> (routing key / binding) -> Queue -> Consumer.
 * <p>
 * Por que não mandar direto pra fila? Porque o Exchange permite desacoplar o producer
 * do destino real da mensagem: no futuro dá pra ter várias filas ouvindo o mesmo Exchange
 * (ex: uma fila de log e uma fila de processamento) sem mudar quem publica.
 */
@Configuration
public class RabbitMQConfig {

    public static final String NOTIFICATION_EXCHANGE = "clientefacil.notification.exchange";
    public static final String NOTIFICATION_QUEUE = "clientefacil.notification.queue";
    public static final String NOTIFICATION_ROUTING_KEY = "clientefacil.notification";

    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE);
    }

    // true = durable: a fila sobrevive a um restart do RabbitMQ (mensagens não se perdem).
    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    // Liga a fila ao exchange: só mensagens publicadas com NOTIFICATION_ROUTING_KEY chegam nela.
    @Bean
    public Binding notificationBinding(Queue notificationQueue, DirectExchange notificationExchange) {
        return BindingBuilder.bind(notificationQueue).to(notificationExchange).with(NOTIFICATION_ROUTING_KEY);
    }

    // Sem isso, o RabbitTemplate enviaria/receberia bytes crus (java.io.Serializable).
    // Com o Jackson2JsonMessageConverter, objetos Java viram JSON automaticamente.
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // RabbitTemplate é o cliente usado para PUBLICAR mensagens (ver NotificationPublisher).
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
