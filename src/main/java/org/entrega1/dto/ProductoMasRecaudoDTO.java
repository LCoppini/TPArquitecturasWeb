package org.entrega1.dto;

import lombok.*;
import org.entrega1.entity.Producto;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString

public class ProductoMasRecaudoDTO extends Producto {

    private String nombre;
    private float totalRecaudado;

}
