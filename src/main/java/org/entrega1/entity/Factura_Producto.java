package org.entrega1.entity;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString

public class Factura_Producto {
    private Long id;//id necesario para representar de manera mas sencilla la relacion entre idProducto y idFactura
    private Long idFactura;
    private Long idProducto;
    private Integer cantidad;
}
