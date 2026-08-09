package br.com.clientefacil.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * `extractDeathInfo` lê o header "x-death" que o próprio RabbitMQ adiciona — um formato de lista de
 * mapas heterogênea, sem garantia forte do broker (ver javadoc do método). É o parsing mais
 * defensivo do projeto (tolera header ausente, malformado, ou com campos individuais faltando), o
 * que o torna fácil de quebrar silenciosamente numa refatoração — daí valer teste dedicado.
 */
class NotificationDeadLetterListenerTest {

    private final NotificationDeadLetterListener listener =
            new NotificationDeadLetterListener(null, null, null, null, null);

    @Test
    void parsesReasonCountAndTime_whenHeaderIsWellFormed() {
        Date time = new Date();
        Message message = messageWithXDeath(Map.of("reason", "rejected", "count", 3L, "time", time));

        var result = listener.extractDeathInfo(message);

        assertThat(result.reason()).isEqualTo("rejected");
        assertThat(result.count()).isEqualTo(3);
        assertThat(result.failedAt()).isEqualTo(LocalDateTime.ofInstant(time.toInstant(), java.time.ZoneId.systemDefault()));
    }

    @Test
    void acceptsCountAsAnyNumberSubtype() {
        // O AMQP pode decodificar números como Integer, Long, etc dependendo do encoder — o parsing
        // usa "instanceof Number", não um tipo específico.
        Message message = messageWithXDeath(Map.of("reason", "expired", "count", 5));

        var result = listener.extractDeathInfo(message);

        assertThat(result.count()).isEqualTo(5);
    }

    @Test
    void defaultsToNullReasonAndCount_butKeepsNowAsFailedAt_whenHeaderMissing() {
        Message message = new Message("corpo".getBytes(), new MessageProperties());

        var result = listener.extractDeathInfo(message);

        assertThat(result.reason()).isNull();
        assertThat(result.count()).isNull();
        assertThat(result.failedAt()).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS));
    }

    @Test
    void defaultsGracefully_whenXDeathIsAnEmptyList() {
        MessageProperties properties = new MessageProperties();
        properties.getHeaders().put("x-death", List.of());
        Message message = new Message("corpo".getBytes(), properties);

        var result = listener.extractDeathInfo(message);

        assertThat(result.reason()).isNull();
        assertThat(result.count()).isNull();
        assertThat(result.failedAt()).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS));
    }

    @Test
    void defaultsGracefully_withoutThrowing_whenXDeathIsNotAList() {
        MessageProperties properties = new MessageProperties();
        properties.getHeaders().put("x-death", "isso não deveria ser uma string");
        Message message = new Message("corpo".getBytes(), properties);

        var result = listener.extractDeathInfo(message);

        assertThat(result.reason()).isNull();
        assertThat(result.count()).isNull();
    }

    @Test
    void defaultsGracefully_withoutThrowing_whenFirstDeathEntryIsNotAMap() {
        MessageProperties properties = new MessageProperties();
        properties.getHeaders().put("x-death", List.of("também não deveria ser uma string"));
        Message message = new Message("corpo".getBytes(), properties);

        var result = listener.extractDeathInfo(message);

        assertThat(result.reason()).isNull();
        assertThat(result.count()).isNull();
    }

    @Test
    void defaultsFailedAtToNow_whenTimeFieldIsMissing_butStillParsesOtherFields() {
        Map<String, Object> death = new HashMap<>();
        death.put("reason", "rejected");
        // sem "time" nem "count" de propósito
        Message message = messageWithXDeath(death);

        var result = listener.extractDeathInfo(message);

        assertThat(result.reason()).isEqualTo("rejected");
        assertThat(result.count()).isNull();
        assertThat(result.failedAt()).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS));
    }

    private Message messageWithXDeath(Map<String, Object> deathEntry) {
        MessageProperties properties = new MessageProperties();
        properties.getHeaders().put("x-death", List.of(deathEntry));
        return new Message("corpo".getBytes(), properties);
    }
}
