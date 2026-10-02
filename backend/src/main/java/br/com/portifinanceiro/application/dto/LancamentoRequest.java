package br.com.portifinanceiro.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record LancamentoRequest(
    @NotBlank String tipo,
    @NotNull LocalDate data,
    @NotBlank @Size(max = 120) String descricao,
    @NotBlank @Size(max = 60) String categoria,
    @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal valor,
    @Size(max = 500) String observacao
) {
}
