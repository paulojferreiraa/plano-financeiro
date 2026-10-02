package br.com.portifinanceiro.application.repository;

import br.com.portifinanceiro.application.model.Receita;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceitaRepository extends JpaRepository<Receita, UUID> {
    List<Receita> findAllByUsuarioId(Long usuarioId);
    Optional<Receita> findByIdAndUsuarioId(UUID id, Long usuarioId);
}
