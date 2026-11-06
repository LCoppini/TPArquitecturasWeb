package com.ticketya.entrada.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class RegistroPagoId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "id_entrada")
    private Long idEntrada;

    @Column(name = "id_pago")
    private Long idPago;
}
