package ch.admin.zas.jweb.laforge.security.service;

import ch.admin.zas.jweb.laforge.common.config.LaForgeProperties;
import freemarker.template.TemplateException;
import jakarta.mail.MessagingException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

/** Envoie les courriels transactionnels de l'authentification (confirmation de compte). */
@Component
public class MailNotifier {

    private static final String VERIFICATION_TEMPLATE = "mail/verification-email.ftlh";

    private final JavaMailSender mailSender;
    private final freemarker.template.Configuration freemarkerConfiguration;
    private final LaForgeProperties properties;

    public MailNotifier(
            JavaMailSender mailSender,
            freemarker.template.Configuration freemarkerConfiguration,
            LaForgeProperties properties) {
        this.mailSender = mailSender;
        this.freemarkerConfiguration = freemarkerConfiguration;
        this.properties = properties;
    }

    public void sendVerificationEmail(String toEmail, String displayName, String rawToken) {
        var link = properties.mail().verificationBaseUrl() + "?token=" + rawToken;
        var model = Map.of("displayName", displayName, "verificationLink", link);
        try {
            var template = freemarkerConfiguration.getTemplate(VERIFICATION_TEMPLATE);
            var html = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setTo(toEmail);
            helper.setFrom(properties.mail().from());
            helper.setSubject("Confirmez votre compte La Forge");
            helper.setText(html, true);
            mailSender.send(message);
        } catch (IOException | TemplateException | MessagingException e) {
            throw new IllegalStateException("Échec d'envoi du courriel de vérification.", e);
        }
    }
}
