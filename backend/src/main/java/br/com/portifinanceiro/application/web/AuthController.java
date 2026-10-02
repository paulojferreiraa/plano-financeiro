package br.com.portifinanceiro.application.web;

import br.com.portifinanceiro.application.dto.CadastroRequest;
import br.com.portifinanceiro.application.dto.LoginRequest;
import br.com.portifinanceiro.application.dto.RecuperacaoSenhaRequest;
import br.com.portifinanceiro.application.dto.RedefinirSenhaRequest;
import br.com.portifinanceiro.application.model.Usuario;
import br.com.portifinanceiro.application.repository.UsuarioRepository;
import br.com.portifinanceiro.application.service.RecuperacaoSenhaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository;
    private final RecuperacaoSenhaService recuperacaoSenha;

    public AuthController(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, SecurityContextRepository contextRepository, RecuperacaoSenhaService recuperacaoSenha) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.contextRepository = contextRepository;
        this.recuperacaoSenha = recuperacaoSenha;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken());
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody CadastroRequest request) {
        String email = normalizarEmail(request.email());
        if (usuarios.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já possui cadastro.");
        }

        Usuario usuario = usuarios.save(new Usuario(
            request.nome().trim(),
            email,
            passwordEncoder.encode(request.senha())
        ));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of("nome", usuario.getNome(), "email", usuario.getEmail()));
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<Map<String, String>> solicitarRecuperacao(@Valid @RequestBody RecuperacaoSenhaRequest request) {
        recuperacaoSenha.solicitar(request.email());
        return ResponseEntity.accepted().body(Map.of(
            "message", "Se o e-mail estiver cadastrado, enviaremos um link para redefinir a senha."
        ));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest request) {
        recuperacaoSenha.redefinir(request.token(), request.senha());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(normalizarEmail(request.email()), request.senha()));

        servletRequest.getSession(true);
        servletRequest.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, servletRequest, servletResponse);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public Map<String, String> currentUser(Authentication authentication) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(authentication.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));
        return Map.of("nome", usuario.getNome(), "email", usuario.getEmail());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return ResponseEntity.noContent().build();
    }

    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
