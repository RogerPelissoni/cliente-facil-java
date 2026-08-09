package br.com.clientefacil.messaging;

import br.com.clientefacil.entity.MailConfig;
import br.com.clientefacil.entity.enums.MailEncryptionType;
import br.com.clientefacil.messaging.template.EmailTemplate;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Lógica de montar e enviar um e-mail a partir de um {@link MailConfig} — extraída do
 * {@link EmailListener} pra ser reaproveitada também pelo teste síncrono de configuração ainda não
 * salva (ver {@code MailConfigService.testDraft}), que não passa pela fila.
 * <p>
 * O JavaMailSender NÃO é um bean único fixo: como a config é dinâmica (por empresa, ou até um
 * rascunho nem persistido), montamos um JavaMailSenderImpl novo a cada envio, a partir do
 * MailConfig recebido.
 */
@Component
@RequiredArgsConstructor
public class EmailSenderService {

    private final TemplateEngine templateEngine;

    // Overload tipado — preferido por qualquer chamador síncrono novo (ver MailConfigService.testDraft).
    // A versão (String, Map) abaixo continua existindo só porque EmailListener recebe os dois já
    // desserializados do JSON da fila (EmailMessageDTO), onde o tipo do record se perde — ali não tem
    // como recuperar o EmailTemplate original.
    public void send(MailConfig config, List<String> to, String subject, EmailTemplate template) throws Exception {
        send(config, to, subject, template.templateName(), template.toVariables());
    }

    public void send(MailConfig config, List<String> to, String subject, String template, Map<String, Object> variables) throws Exception {
        String html = renderTemplate(template, variables);

        JavaMailSenderImpl mailSender = buildMailSender(config);
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
        helper.setFrom(config.getDsFromAddress(), config.getDsFromName());
        helper.setTo(to.toArray(new String[0]));
        helper.setSubject(subject);
        helper.setText(html, true);

        mailSender.send(mimeMessage);
    }

    private String renderTemplate(String template, Map<String, Object> variables) {
        Context context = new Context();
        if (variables != null) {
            context.setVariables(variables);
        }
        return templateEngine.process("email/" + template, context);
    }

    private JavaMailSenderImpl buildMailSender(MailConfig config) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(config.getDsHost());
        sender.setPort(config.getNrPort());
        sender.setUsername(config.getDsUsername());
        sender.setPassword(config.getDsPassword());
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(StringUtils.hasText(config.getDsUsername())));

        if (config.getTpEncryption() == MailEncryptionType.SSL) {
            props.put("mail.smtp.ssl.enable", "true");
        } else if (config.getTpEncryption() == MailEncryptionType.TLS) {
            props.put("mail.smtp.starttls.enable", "true");
        }

        return sender;
    }
}
