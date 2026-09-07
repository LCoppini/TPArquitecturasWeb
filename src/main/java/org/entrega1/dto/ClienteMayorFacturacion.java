package org.entrega1.dto;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString

public class ClienteMayorFacturacion {
    private Long idCliente;
    private String nombre;
    private String email;
    private float totalFacturado;


}
