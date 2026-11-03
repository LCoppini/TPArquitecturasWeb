package org.example.entrega3.DTOs;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ErrorDTO (String message,
                        LocalDateTime date){
}
