package org.entrega1.entity;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString

public class Factura_Producto {
    private Integer idFactura;
    private Integer idProducto;
    private Integer cantidad;
}
