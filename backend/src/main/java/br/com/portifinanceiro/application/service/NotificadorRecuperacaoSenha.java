package br.com.portifinanceiro.application.service;

public interface NotificadorRecuperacaoSenha {
    void enviarLink(String nome, String email, String link);
}
