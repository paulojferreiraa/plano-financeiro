package br.com.portifinanceiro.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LancamentoResponse(
    UUID id,
    String tipo,
    LocalDate data,
    int ano,
    int mes,
    String descricao,
    String categoria,
    BigDecimal valor,
    String observacao
) {
}
