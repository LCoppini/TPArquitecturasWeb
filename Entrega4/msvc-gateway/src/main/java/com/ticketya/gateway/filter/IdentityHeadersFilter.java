package com.ticketya.gateway.filter;

import com.ticketya.gateway.security.GatewaySecurityErrorWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Filtro posterior a Spring Security: descarta la identidad enviada por el
 * cliente y la deriva del JWT. X-User-Role se elimina porque la autorizacion
 * ya la resolvio el gateway (ADR-0001).
 */
@Component
@Order(SecurityProperties.DEFAULT_FILTER_ORDER + 1)
public class IdentityHeadersFilter extends OncePerRequestFilter {

    static final String ID_USUARIO = "X-User-Id";
    static final String ROL_USUARIO = "X-User-Role";
    static final String CLAIM_ID_USUARIO = "idUsuario";

    private final GatewaySecurityErrorWriter writer;

    public IdentityHeadersFilter(GatewaySecurityErrorWriter writer) {
        this.writer = writer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            chain.doFilter(request, response);
            return;
        }

        String idUsuario = jwtAuthentication.getToken().getClaimAsString(CLAIM_ID_USUARIO);
        if (idUsuario == null || idUsuario.isBlank()) {
            writer.write(response, request, HttpStatus.UNAUTHORIZED,
                    "El token no contiene el claim idUsuario.");
            return;
        }

        HttpServletRequest seguro = new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                if (ID_USUARIO.equalsIgnoreCase(name)) {
                    return idUsuario;
                }
                if (ROL_USUARIO.equalsIgnoreCase(name)) {
                    return null;
                }
                return super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                if (ID_USUARIO.equalsIgnoreCase(name)) {
                    return Collections.enumeration(List.of(idUsuario));
                }
                if (ROL_USUARIO.equalsIgnoreCase(name)) {
                    return Collections.emptyEnumeration();
                }
                return super.getHeaders(name);
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                Set<String> nombres = new LinkedHashSet<>();
                Enumeration<String> originales = super.getHeaderNames();
                while (originales.hasMoreElements()) {
                    String nombre = originales.nextElement();
                    if (!ID_USUARIO.equalsIgnoreCase(nombre) && !ROL_USUARIO.equalsIgnoreCase(nombre)) {
                        nombres.add(nombre);
                    }
                }
                nombres.add(ID_USUARIO);
                return Collections.enumeration(nombres);
            }
        };

        chain.doFilter(seguro, response);
    }
}
