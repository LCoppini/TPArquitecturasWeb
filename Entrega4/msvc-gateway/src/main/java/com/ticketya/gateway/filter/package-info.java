/**
 * Filtros transversales:
 *  - IdentityHeadersFilter: sobrescribe X-User-Id desde el JWT y elimina
 *    X-User-Role (E2, ADR-0001).
 *  - Generation/propagacion de X-Trace-Id y log de requests (E2, pendiente).
 */
package com.ticketya.gateway.filter;
