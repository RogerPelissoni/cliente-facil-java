package br.com.clientefacil.service;

import br.com.clientefacil.messaging.EmailMessageDTO;
import br.com.clientefacil.messaging.EmailPublisher;
import br.com.clientefacil.messaging.template.EmailTemplate;
import br.com.clientefacil.messaging.template.TestEmailTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * API reutilizável para qualquer feature do projeto disparar um e-mail — só enfileira (fire-and-
 * forget, mesmo espírito do NotificationPublisher); quem efetivamente monta e envia é o
 * EmailListener, de forma assíncrona.
 * <p>
 * companyId null = e-mail do sistema, usa a config base (ver MailConfigService); companyId
 * preenchido = tenta a config da empresa antes de cair pra base.
 * <p>
 * Recebe um {@link EmailTemplate} tipado, não um `(String, Map)` solto — ver o javadoc de
 * EmailTemplate pra entender o motivo.
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final EmailPublisher publisher;

    public void sendTemplated(Long companyId, List<String> to, String subject, EmailTemplate template) {
        publisher.publish(new EmailMessageDTO(companyId, to, subject, template.templateName(), template.toVariables()));
    }

    public void sendTemplated(Long companyId, String to, String subject, EmailTemplate template) {
        sendTemplated(companyId, List.of(to), subject, template);
    }

    // Usado pelo MailConfigController (/mail-configs/test) pra validar uma config sem precisar de
    // um gatilho de negócio real.
    public void sendTest(Long companyId, String to) {
        var template = new TestEmailTemplate(LocalDateTime.now().format(DATE_FORMAT));
        sendTemplated(companyId, to, "Cliente Fácil — e-mail de teste", template);
    }
}
