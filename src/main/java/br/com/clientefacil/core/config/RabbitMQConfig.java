package br.com.clientefacil.core.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
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
 * <p>
 * Retry + Dead Letter Queue: se o NotificationListener lançar uma exceção, o Spring AMQP
 * (configurado em application.yml, spring.rabbitmq.listener.simple.retry) tenta reprocessar a
 * mesma mensagem até 3 vezes, com backoff crescente (1s, 2s, 4s...), antes de desistir — em vez
 * de martelar o listener imediatamente feito o comportamento padrão faria. Se mesmo assim
 * continuar falhando, a mensagem é rejeitada sem reenfileirar
 * (default-requeue-rejected: false) e, por causa do "x-dead-letter-exchange" configurado na
 * fila principal, o próprio RabbitMQ a redireciona para a DLQ abaixo — em vez de reprocessar
 * pra sempre (o que acontecia antes) ou descartar silenciosamente.
 */
@Configuration
public class RabbitMQConfig {

    public static final String NOTIFICATION_EXCHANGE = "clientefacil.notification.exchange";
    public static final String NOTIFICATION_QUEUE = "clientefacil.notification.queue";
    public static final String NOTIFICATION_ROUTING_KEY = "clientefacil.notification";

    public static final String NOTIFICATION_DLX = "clientefacil.notification.dlx";
    public static final String NOTIFICATION_DLQ = "clientefacil.notification.queue.dlq";
    public static final String NOTIFICATION_DLQ_ROUTING_KEY = "clientefacil.notification.dead";

    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE);
    }

    // durable = sobrevive a um restart do RabbitMQ. Os argumentos x-dead-letter-* dizem ao
    // RabbitMQ: "se uma mensagem desta fila for rejeitada sem reenfileirar, mande pra DLX
    // abaixo em vez de descartar" — é isso que conecta essa fila à DLQ.
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_DLX)
                .withArgument("x-dead-letter-routing-key", NOTIFICATION_DLQ_ROUTING_KEY)
                .build();
    }

    // Liga a fila ao exchange: só mensagens publicadas com NOTIFICATION_ROUTING_KEY chegam nela.
    @Bean
    public Binding notificationBinding(Queue notificationQueue, DirectExchange notificationExchange) {
        return BindingBuilder.bind(notificationQueue).to(notificationExchange).with(NOTIFICATION_ROUTING_KEY);
    }

    // Exchange/fila dedicados a mensagens que esgotaram as tentativas de reprocessamento.
    // Continuam inspecionáveis pelo painel do RabbitMQ (http://localhost:15672), mas também são
    // consumidas por NotificationDeadLetterListener, que grava um registro mínimo em banco
    // (notification_dead_letter) e emite um alerta em tempo real via WebSocket — reprocessamento
    // automático continua fora do escopo, de propósito (ver docs/mensageria-e-websocket.md).
    @Bean
    public DirectExchange notificationDlx() {
        return new DirectExchange(NOTIFICATION_DLX);
    }

    @Bean
    public Queue notificationDlq() {
        return QueueBuilder.durable(NOTIFICATION_DLQ).build();
    }

    @Bean
    public Binding notificationDlqBinding(Queue notificationDlq, DirectExchange notificationDlx) {
        return BindingBuilder.bind(notificationDlq).to(notificationDlx).with(NOTIFICATION_DLQ_ROUTING_KEY);
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
