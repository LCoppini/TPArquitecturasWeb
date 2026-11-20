/**
 * E2: autenticacion JWT RS256, matriz rol -> path y errores 401/403 uniformes.
 * La cadena es servlet (SecurityFilterChain con HttpSecurity) porque el gateway
 * usa Spring Cloud Gateway Server WebMVC. La identidad se toma del claim
 * idUsuario; X-User-Role no se propaga (ADR-0001).
 */
package com.ticketya.gateway.security;
