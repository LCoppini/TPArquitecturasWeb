package com.ticketya.preferencias.service;

import com.ticketya.preferencias.dto.response.RecomendacionResponse;

import java.util.List;

public interface RecomendacionService {

    List<RecomendacionResponse> recomendar(Long idUsuario);
}
