package br.com.portifinanceiro.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"demo", "test"})
public class NotificadorDemoRecuperacaoSenha implements NotificadorRecuperacaoSenha {
    private static final Logger logger = LoggerFactory.getLogger(NotificadorDemoRecuperacaoSenha.class);

    @Override
    public void enviarLink(String nome, String email, String link) {
        logger.warn("Link de recuperacao local para {} ({}): {}", nome, email, link);
    }
}
