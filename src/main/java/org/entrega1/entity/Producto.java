package org.entrega1.entity;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString

public class Producto {
    private Long idProducto;
    private String nombre;
    private Float valor;
}
