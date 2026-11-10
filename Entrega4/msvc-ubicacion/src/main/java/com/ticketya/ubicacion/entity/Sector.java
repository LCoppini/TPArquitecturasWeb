package com.ticketya.ubicacion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Estructura fisica de un sector. No guarda precio (vive en msvc-evento) ni
 * disponibilidad (vive en msvc-entrada). Su id debe ser estable: otros servicios lo guardan sin FK.
 */
@Entity
@Table(
        name = "sector",
        uniqueConstraints = @UniqueConstraint(name = "uk_sector_lugar_nombre", columnNames = {"id_lugar", "nombre"})
)
@Getter
@Setter
@NoArgsConstructor
public class Sector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_lugar", nullable = false)
    private Lugar lugar;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoSector tipo;

    /** Solo sectores numerados. */
    private Integer filas;

    /** Solo sectores numerados. */
    @Column(name = "asientos_por_fila")
    private Integer asientosPorFila;

    /** NUMERADO: filas * asientosPorFila. NO_NUMERADO: capacidad informada. */
    @Column(name = "capacidad_max", nullable = false)
    private Integer capacidadMax;

    public Long getIdLugar() {
        return lugar == null ? null : lugar.getId();
    }
}
