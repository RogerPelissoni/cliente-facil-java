package br.com.clientefacil.core.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exchange/fila dedicados ao envio assíncrono de e-mail — mesmo padrão de RabbitMQConfig (Producer
 * -> Exchange -> Queue -> Consumer, com retry+DLQ), mas isolados numa fila própria, não a fila de
 * notificações.
 * <p>
 * Por que uma fila própria em vez de reaproveitar a de notificações? Porque a DLQ de notificações
 * já é usada por NotificationDeadLetterListener para gerar um alerta de e-mail (ver
 * EmailIntegration em NotificationDeadLetterListener) — se o envio de e-mail também passasse pela
 * fila/DLQ de notificação, uma falha sistêmica (ex: SMTP fora do ar) poderia fazer esse próprio
 * alerta cair na DLQ de novo, dependendo de quem consome o quê, criando um acoplamento frágil entre
 * dois domínios de infraestrutura diferentes. Filas separadas mantêm os dois pipelines
 * independentes: um e-mail que falha rejeita/reenfileira só dentro do seu próprio
 * retry+DLQ, sem qualquer risco de realimentar o pipeline de notificações.
 * <p>
 * O retry (3 tentativas, backoff 1s/2s/4s) e o "default-requeue-rejected: false" são globais
 * (spring.rabbitmq.listener.simple.retry em application.yml) e valem para qualquer @RabbitListener
 * do projeto, incluindo o EmailListener.
 */
@Configuration
public class EmailRabbitMQConfig {

    public static final String EMAIL_EXCHANGE = "clientefacil.email.exchange";
    public static final String EMAIL_QUEUE = "clientefacil.email.queue";
    public static final String EMAIL_ROUTING_KEY = "clientefacil.email";

    public static final String EMAIL_DLX = "clientefacil.email.dlx";
    public static final String EMAIL_DLQ = "clientefacil.email.queue.dlq";
    public static final String EMAIL_DLQ_ROUTING_KEY = "clientefacil.email.dead";

    @Bean
    public DirectExchange emailExchange() {
        return new DirectExchange(EMAIL_EXCHANGE);
    }

    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", EMAIL_DLX)
                .withArgument("x-dead-letter-routing-key", EMAIL_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding emailBinding(Queue emailQueue, DirectExchange emailExchange) {
        return BindingBuilder.bind(emailQueue).to(emailExchange).with(EMAIL_ROUTING_KEY);
    }

    @Bean
    public DirectExchange emailDlx() {
        return new DirectExchange(EMAIL_DLX);
    }

    @Bean
    public Queue emailDlq() {
        return QueueBuilder.durable(EMAIL_DLQ).build();
    }

    @Bean
    public Binding emailDlqBinding(Queue emailDlq, DirectExchange emailDlx) {
        return BindingBuilder.bind(emailDlq).to(emailDlx).with(EMAIL_DLQ_ROUTING_KEY);
    }
}
