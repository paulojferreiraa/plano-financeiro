package br.com.portifinanceiro.application.service;

import br.com.portifinanceiro.application.model.TokenRecuperacaoSenha;
import br.com.portifinanceiro.application.model.Usuario;
import br.com.portifinanceiro.application.repository.TokenRecuperacaoSenhaRepository;
import br.com.portifinanceiro.application.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RecuperacaoSenhaService {
    private static final Logger logger = LoggerFactory.getLogger(RecuperacaoSenhaService.class);
    private static final SecureRandom secureRandom = new SecureRandom();

    private final UsuarioRepository usuarios;
    private final TokenRecuperacaoSenhaRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final NotificadorRecuperacaoSenha notificador;
    private final String urlBase;
    private final long validadeMinutos;

    public RecuperacaoSenhaService(
        UsuarioRepository usuarios,
        TokenRecuperacaoSenhaRepository tokens,
        PasswordEncoder passwordEncoder,
        NotificadorRecuperacaoSenha notificador,
        @Value("${app.base-url}") String urlBase,
        @Value("${app.password-reset.expiration-minutes:30}") long validadeMinutos
    ) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.notificador = notificador;
        this.urlBase = urlBase.replaceAll("/+$", "");
        this.validadeMinutos = validadeMinutos;
    }

    @Transactional
    public void solicitar(String email) {
        usuarios.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT)).ifPresent(usuario -> {
            tokens.deleteAllByUsuarioId(usuario.getId());
            String token = criarToken();
            Instant expiraEm = Instant.now().plus(Duration.ofMinutes(validadeMinutos));
            tokens.save(new TokenRecuperacaoSenha(usuario, hashToken(token), expiraEm));

            String link = urlBase + "/#recuperar=" + token;
            try {
                notificador.enviarLink(usuario.getNome(), usuario.getEmail(), link);
            } catch (RuntimeException exception) {
                logger.error("Nao foi possivel enviar o link de recuperacao para o usuario {}.", usuario.getId(), exception);
            }
        });
    }

    @Transactional
    public void redefinir(String token, String novaSenha) {
        TokenRecuperacaoSenha tokenSalvo = tokens.buscarPorHashParaAtualizacao(hashToken(token))
            .orElseThrow(RecuperacaoSenhaService::tokenInvalido);
        if (!tokenSalvo.getExpiraEm().isAfter(Instant.now())) {
            throw tokenInvalido();
        }

        Usuario usuario = tokenSalvo.getUsuario();
        usuario.atualizarSenhaHash(passwordEncoder.encode(novaSenha));
        usuarios.save(usuario);
        tokens.delete(tokenSalvo);
    }

    private static String criarToken() {
        byte[] valor = new byte[32];
        secureRandom.nextBytes(valor);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(valor);
    }

    private static String hashToken(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 nao esta disponivel.", exception);
        }
    }

    private static ResponseStatusException tokenInvalido() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Link inválido ou expirado. Solicite uma nova recuperação de senha.");
    }
}
