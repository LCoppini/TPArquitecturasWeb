package com.ticketya.entrada.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pagos")
@Getter
@Setter
@NoArgsConstructor
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Long idPago;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(name = "estado", nullable = false, length = 30)
    private String estado;

    @Column(name = "medio_de_pago", nullable = false, length = 50)
    private String medioDePago;

    @Column(name = "id_entrada", nullable = false)
    private Long idEntrada;

    @Column(name = "clave_idempotencia", length = 100)
    private String claveIdempotencia;

    @Column(name = "referencia_pasarela", length = 120)
    private String referenciaPasarela;

    @Column(name = "huella_solicitud", length = 64)
    private String huellaSolicitud;

    @OneToMany(mappedBy = "pago")
    private List<RegistroPago> registrosPago = new ArrayList<>();
}
