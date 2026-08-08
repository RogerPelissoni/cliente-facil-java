package br.com.clientefacil.service;

import br.com.clientefacil.messaging.EmailMessageDTO;
import br.com.clientefacil.messaging.EmailPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * API reutilizável para qualquer feature do projeto disparar um e-mail — só enfileira (fire-and-
 * forget, mesmo espírito do NotificationPublisher); quem efetivamente monta e envia é o
 * EmailListener, de forma assíncrona.
 * <p>
 * companyId null = e-mail do sistema, usa a config base (ver MailConfigService); companyId
 * preenchido = tenta a config da empresa antes de cair pra base.
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final EmailPublisher publisher;

    public void sendTemplated(Long companyId, List<String> to, String subject, String template, Map<String, Object> variables) {
        publisher.publish(new EmailMessageDTO(companyId, to, subject, template, variables));
    }

    public void sendTemplated(Long companyId, String to, String subject, String template, Map<String, Object> variables) {
        sendTemplated(companyId, List.of(to), subject, template, variables);
    }

    // Usado pelo MailConfigController (/mail-configs/test) pra validar uma config sem precisar de
    // um gatilho de negócio real.
    public void sendTest(Long companyId, String to) {
        Map<String, Object> variables = Map.of("sentAt", LocalDateTime.now().format(DATE_FORMAT));
        sendTemplated(companyId, to, "Cliente Fácil — e-mail de teste", "test-email", variables);
    }
}
