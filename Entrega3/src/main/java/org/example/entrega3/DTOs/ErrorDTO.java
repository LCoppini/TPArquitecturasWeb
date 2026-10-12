package org.example.entrega3.DTOs;

import java.time.LocalDate;

public record ErrorDTO (String message,
                        LocalDate date){
}
