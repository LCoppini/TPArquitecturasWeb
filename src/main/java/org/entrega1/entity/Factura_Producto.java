package org.entrega1.entity;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString

public class Factura_Producto {
    private Long idFactura;
    private Long idProducto;
    private Integer cantidad;
}
