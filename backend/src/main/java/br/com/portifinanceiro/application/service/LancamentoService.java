package br.com.portifinanceiro.application.service;

import br.com.portifinanceiro.application.dto.LancamentoRequest;
import br.com.portifinanceiro.application.dto.LancamentoResponse;
import br.com.portifinanceiro.application.model.Despesa;
import br.com.portifinanceiro.application.model.Receita;
import br.com.portifinanceiro.application.model.Usuario;
import br.com.portifinanceiro.application.repository.DespesaRepository;
import br.com.portifinanceiro.application.repository.ReceitaRepository;
import br.com.portifinanceiro.application.repository.UsuarioRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LancamentoService {
    private final UsuarioRepository usuarios;
    private final ReceitaRepository receitas;
    private final DespesaRepository despesas;

    public LancamentoService(UsuarioRepository usuarios, ReceitaRepository receitas, DespesaRepository despesas) {
        this.usuarios = usuarios;
        this.receitas = receitas;
        this.despesas = despesas;
    }

    @Transactional(readOnly = true)
    public List<LancamentoResponse> listar(String email) {
        Usuario usuario = obterUsuario(email);
        return java.util.stream.Stream.concat(
                receitas.findAllByUsuarioId(usuario.getId()).stream().map(this::paraResposta),
                despesas.findAllByUsuarioId(usuario.getId()).stream().map(this::paraResposta))
            .sorted(Comparator.comparing(LancamentoResponse::data).thenComparing(LancamentoResponse::id))
            .toList();
    }

    @Transactional
    public LancamentoResponse criar(String email, LancamentoRequest request) {
        Usuario usuario = obterUsuario(email);
        return switch (tipo(request)) {
            case "receita" -> paraResposta(receitas.save(new Receita(usuario, request.descricao().trim(), request.categoria(), request.valor(), request.data(), request.observacao())));
            case "despesa" -> paraResposta(despesas.save(new Despesa(usuario, request.descricao().trim(), request.categoria(), request.valor(), request.data(), request.observacao())));
            default -> throw tipoInvalido();
        };
    }

    @Transactional
    public LancamentoResponse atualizar(String email, UUID id, LancamentoRequest request) {
        Usuario usuario = obterUsuario(email);
        String tipo = tipo(request);

        Receita receita = receitas.findByIdAndUsuarioId(id, usuario.getId()).orElse(null);
        if (receita != null) {
            if (tipo.equals("receita")) {
                receita.atualizar(request.descricao().trim(), request.categoria(), request.valor(), request.data(), request.observacao());
                return paraResposta(receita);
            }
            receitas.delete(receita);
            return paraResposta(despesas.save(new Despesa(usuario, request.descricao().trim(), request.categoria(), request.valor(), request.data(), request.observacao())));
        }

        Despesa despesa = despesas.findByIdAndUsuarioId(id, usuario.getId()).orElse(null);
        if (despesa != null) {
            if (tipo.equals("despesa")) {
                despesa.atualizar(request.descricao().trim(), request.categoria(), request.valor(), request.data(), request.observacao());
                return paraResposta(despesa);
            }
            despesas.delete(despesa);
            return paraResposta(receitas.save(new Receita(usuario, request.descricao().trim(), request.categoria(), request.valor(), request.data(), request.observacao())));
        }

        throw lancamentoNaoEncontrado();
    }

    @Transactional
    public void excluir(String email, UUID id) {
        Usuario usuario = obterUsuario(email);
        Receita receita = receitas.findByIdAndUsuarioId(id, usuario.getId()).orElse(null);
        if (receita != null) {
            receitas.delete(receita);
            return;
        }
        Despesa despesa = despesas.findByIdAndUsuarioId(id, usuario.getId()).orElseThrow(LancamentoService::lancamentoNaoEncontrado);
        despesas.delete(despesa);
    }

    private Usuario obterUsuario(String email) {
        return usuarios.findByEmailIgnoreCase(email).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));
    }

    private static String tipo(LancamentoRequest request) {
        return request.tipo() == null ? "" : request.tipo().trim().toLowerCase(Locale.ROOT);
    }

    private static ResponseStatusException tipoInvalido() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "O tipo deve ser receita ou despesa.");
    }

    private static ResponseStatusException lancamentoNaoEncontrado() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Lançamento não encontrado.");
    }

    private LancamentoResponse paraResposta(Receita receita) {
        return new LancamentoResponse(receita.getId(), "receita", receita.getData(), receita.getData().getYear(), receita.getData().getMonthValue() - 1, receita.getDescricao(), receita.getCategoria(), receita.getValor(), receita.getObservacao());
    }

    private LancamentoResponse paraResposta(Despesa despesa) {
        return new LancamentoResponse(despesa.getId(), "despesa", despesa.getData(), despesa.getData().getYear(), despesa.getData().getMonthValue() - 1, despesa.getDescricao(), despesa.getCategoria(), despesa.getValor(), despesa.getObservacao());
    }
}
