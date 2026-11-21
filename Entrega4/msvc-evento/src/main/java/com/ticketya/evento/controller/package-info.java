/**
 * CAPA DE PRESENTACION. Controllers REST (@RestController) + anotaciones OpenAPI (@Operation, @ApiResponse).
 * Reciben el HTTP, validan la entrada (@Valid) y delegan en el service. SIN logica de negocio y SIN usar repositories.
 *
 * Endpoints:
 *  - EventoController: GET publico del catalogo y de disponibilidad (/api/eventos/**)
 *  - OrganizadorEventoController: escrituras propias (/api/organizador/eventos/**)
 *  - AdminEventoController: moderacion (/api/admin/eventos/**)
 *  - ParticipanteController: consulta (/api/artistas/**)
 *  - AdminParticipanteController: administracion (/api/admin/artistas/**)
 *
 * El gateway autoriza estos prefijos por rol (ADR-0001); los controllers solo
 * leen X-User-Id y no reciben X-User-Role. La disponibilidad publica delega en
 * el contrato interno /internal/entradas/disponibilidad de msvc-entrada.
 */
package com.ticketya.evento.controller;
