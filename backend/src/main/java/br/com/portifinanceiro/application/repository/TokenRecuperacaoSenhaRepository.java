package br.com.portifinanceiro.application.repository;

import br.com.portifinanceiro.application.model.TokenRecuperacaoSenha;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TokenRecuperacaoSenhaRepository extends JpaRepository<TokenRecuperacaoSenha, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from TokenRecuperacaoSenha token where token.tokenHash = :tokenHash")
    Optional<TokenRecuperacaoSenha> buscarPorHashParaAtualizacao(@Param("tokenHash") String tokenHash);

    void deleteAllByUsuarioId(Long usuarioId);
}
