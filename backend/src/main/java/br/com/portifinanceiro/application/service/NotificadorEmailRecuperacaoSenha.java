package br.com.portifinanceiro.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Profile("!demo & !test")
public class NotificadorEmailRecuperacaoSenha implements NotificadorRecuperacaoSenha {
    private final JavaMailSender mailSender;
    private final String remetente;

    public NotificadorEmailRecuperacaoSenha(JavaMailSender mailSender, @Value("${app.mail.from}") String remetente) {
        this.mailSender = mailSender;
        this.remetente = remetente;
    }

    @Override
    @Async
    public void enviarLink(String nome, String email, String link) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(email);
        mensagem.setSubject("Redefinicao da senha do Meu Financeiro");
        mensagem.setText("Ola, " + nome + ".\n\nUse o link abaixo para escolher uma nova senha. Ele expira em 30 minutos e pode ser usado uma unica vez.\n\n" + link + "\n\nSe voce nao solicitou a redefinicao, ignore este e-mail.");
        mailSender.send(mensagem);
    }
}
