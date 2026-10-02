package br.com.portifinanceiro.application.repository;

import br.com.portifinanceiro.application.model.Despesa;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DespesaRepository extends JpaRepository<Despesa, UUID> {
    List<Despesa> findAllByUsuarioId(Long usuarioId);
    Optional<Despesa> findByIdAndUsuarioId(UUID id, Long usuarioId);
}
