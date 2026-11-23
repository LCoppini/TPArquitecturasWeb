package com.ticketya.entrada.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "entrada_fila",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_entrada_fila_evento_secuencia",
                columnNames = {"id_evento", "secuencia"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class IngresoFila {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_evento", nullable = false)
    private Long idEvento;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(name = "secuencia", nullable = false)
    private Long secuencia;

    @Column(name = "estado", nullable = false, length = 24)
    private String estado;

    @Column(name = "ingresado_en", nullable = false)
    private Instant ingresadoEn;

    @Column(name = "vence_en")
    private Instant venceEn;

    @Column(name = "id_tanda")
    private Long idTanda;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /** La secuencia se asigna al entrar a la fila y no puede cambiar después. */
    public void setSecuencia(Long secuencia) {
        if (this.secuencia != null && !this.secuencia.equals(secuencia)) {
            throw new IllegalStateException("La secuencia de la fila es inmutable");
        }
        this.secuencia = secuencia;
    }
}
