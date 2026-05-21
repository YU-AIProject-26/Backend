package com.acta.springserver.domain.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendVerificationCodeEmail(String toEmail, String code) {
        String subject = "[Acta] 이메일 인증 코드 안내";
        String htmlContent = buildSignupVerificationEmailHtml(code);
        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    public void sendPasswordResetCodeEmail(String toEmail, String code) {
        String subject = "[Acta] 비밀번호 재설정 인증 코드 안내";
        String htmlContent = buildPasswordResetEmailHtml(code);
        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    private void sendHtmlEmail(String toEmail, String subject, String htmlContent) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setFrom(fromEmail);
            helper.setText(htmlContent, true);

            javaMailSender.send(message);
        } catch (MessagingException | MailException e) {
            throw new RuntimeException("이메일 발송 중 오류가 발생했습니다.", e);
        }
    }

    private String buildSignupVerificationEmailHtml(String code) {
        return """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <title>Acta 이메일 인증</title>
                </head>
                <body style="margin:0; padding:0; background-color:#f5f7fb; font-family:Arial, sans-serif;">
                    <div style="max-width:600px; margin:40px auto; background-color:#ffffff; border:1px solid #e5e7eb; border-radius:16px; overflow:hidden;">
                        <div style="padding:32px; text-align:center; background-color:#111827;">
                            <h1 style="margin:0; font-size:28px; color:#ffffff;">Acta</h1>
                        </div>

                        <div style="padding:40px 32px;">
                            <h2 style="margin:0 0 16px; font-size:24px; color:#111827;">이메일 인증 코드 안내</h2>
                            <p style="margin:0 0 12px; font-size:16px; line-height:1.6; color:#374151;">
                                안녕하세요. Acta 회원가입을 위한 이메일 인증 코드입니다.
                            </p>
                            <p style="margin:0 0 24px; font-size:16px; line-height:1.6; color:#374151;">
                                아래 인증 코드를 회원가입 화면에 입력해주세요.
                            </p>

                            <div style="margin:24px 0; padding:20px; text-align:center; background-color:#f9fafb; border:1px solid #d1d5db; border-radius:12px;">
                                <span style="font-size:32px; font-weight:700; letter-spacing:8px; color:#2563eb;">%s</span>
                            </div>

                            <p style="margin:24px 0 8px; font-size:14px; line-height:1.6; color:#6b7280;">
                                인증 코드는 3분 동안 유효합니다.
                            </p>
                            <p style="margin:0; font-size:14px; line-height:1.6; color:#6b7280;">
                                본인이 요청하지 않았다면 이 이메일을 무시하셔도 됩니다.
                            </p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(code);
    }

    private String buildPasswordResetEmailHtml(String code) {
        return """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <title>Acta 비밀번호 재설정</title>
                </head>
                <body style="margin:0; padding:0; background-color:#f5f7fb; font-family:Arial, sans-serif;">
                    <div style="max-width:600px; margin:40px auto; background-color:#ffffff; border:1px solid #e5e7eb; border-radius:16px; overflow:hidden;">
                        <div style="padding:32px; text-align:center; background-color:#111827;">
                            <h1 style="margin:0; font-size:28px; color:#ffffff;">Acta</h1>
                        </div>

                        <div style="padding:40px 32px;">
                            <h2 style="margin:0 0 16px; font-size:24px; color:#111827;">비밀번호 재설정 인증 코드 안내</h2>
                            <p style="margin:0 0 12px; font-size:16px; line-height:1.6; color:#374151;">
                                안녕하세요. Acta 비밀번호 재설정을 위한 인증 코드입니다.
                            </p>
                            <p style="margin:0 0 24px; font-size:16px; line-height:1.6; color:#374151;">
                                아래 인증 코드를 비밀번호 찾기 화면에 입력해주세요.
                            </p>

                            <div style="margin:24px 0; padding:20px; text-align:center; background-color:#f9fafb; border:1px solid #d1d5db; border-radius:12px;">
                                <span style="font-size:32px; font-weight:700; letter-spacing:8px; color:#2563eb;">%s</span>
                            </div>

                            <p style="margin:24px 0 8px; font-size:14px; line-height:1.6; color:#6b7280;">
                                인증 코드는 3분 동안 유효합니다.
                            </p>
                            <p style="margin:0; font-size:14px; line-height:1.6; color:#6b7280;">
                                본인이 요청하지 않았다면 이 이메일을 무시하셔도 됩니다.
                            </p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(code);
    }
}