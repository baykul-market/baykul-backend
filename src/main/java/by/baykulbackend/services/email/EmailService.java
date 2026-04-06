package by.baykulbackend.services.email;

import by.baykulbackend.database.dao.user.Localization;
import jakarta.mail.internet.MimeMessage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Getter
    @Value("${app.url.base-url:https://market.baykul.ru}")
    private String baseUrl;

    private static final String UTF_8 = "UTF-8";

    @Async
    public void sendEmail(String to, String subject, String templateName, Localization localization, Context context) {
        try {
            String localizedTemplate = templateName + "_" + localization.name();
            String htmlContent = templateEngine.process(localizedTemplate, context);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, UTF_8);
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent to: {}, template: {}", to, localizedTemplate);
        } catch (Exception e) {
            log.error("Failed to send email to: {}, template: {}", to, templateName, e);
        }
    }

}