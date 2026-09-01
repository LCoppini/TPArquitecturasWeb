package org.entrega1.entity;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString

public class Cliente {
    private Integer idCliente;
    private String nombre;
    private String email;
}
