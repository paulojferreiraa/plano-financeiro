package br.com.portifinanceiro.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecuperacaoSenhaRequest(
    @NotBlank @Email @Size(max = 254) String email
) {
}
