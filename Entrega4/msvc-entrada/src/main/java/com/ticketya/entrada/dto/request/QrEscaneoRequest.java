package com.ticketya.entrada.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Solicitud de validación de QR en el control de acceso (EP-16). */
@Getter
@Setter
@NoArgsConstructor
public class QrEscaneoRequest {

    @NotNull
    private Long idEvento;

    @NotBlank
    private String codigoQr;
}
