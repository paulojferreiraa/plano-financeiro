package br.com.portifinanceiro.application.web;

import br.com.portifinanceiro.application.dto.LancamentoRequest;
import br.com.portifinanceiro.application.dto.LancamentoResponse;
import br.com.portifinanceiro.application.service.LancamentoService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lancamentos")
public class LancamentoController {
    private final LancamentoService lancamentos;

    public LancamentoController(LancamentoService lancamentos) {
        this.lancamentos = lancamentos;
    }

    @GetMapping
    public List<LancamentoResponse> listar(Authentication authentication) {
        return lancamentos.listar(authentication.getName());
    }

    @PostMapping
    public ResponseEntity<LancamentoResponse> criar(Authentication authentication, @Valid @RequestBody LancamentoRequest request) {
        LancamentoResponse salvo = lancamentos.criar(authentication.getName(), request);
        return ResponseEntity.created(URI.create("/api/lancamentos/" + salvo.id())).body(salvo);
    }

    @PutMapping("/{id}")
    public LancamentoResponse atualizar(Authentication authentication, @PathVariable UUID id, @Valid @RequestBody LancamentoRequest request) {
        return lancamentos.atualizar(authentication.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(Authentication authentication, @PathVariable UUID id) {
        lancamentos.excluir(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
