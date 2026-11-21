package com.ticketya.gateway.config;

import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtAudienceValidator;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Spring Boot no enlaza la property `spring.security.oauth2.resourceserver.jwt.issuer`
 * con el validador de `iss`: usar `issuer-uri` dispararia discovery OIDC, que no
 * existe en este proyecto (ADR-0003). Este post-procesor agrega al decoder real
 * las validaciones de emisor y audiencia ademas de la de timestamp.
 *
 * <p>Solo toca decoders Nimbus. Los decoders de prueba inyectados por los tests
 * de la cadena HTTP quedan intactos, incluso cuando no definen las variables
 * JWT en el environment.
 */
@Component
public class JwtDecoderValidatorPostProcessor implements BeanPostProcessor {

    private static final String PROPIEDAD_ISSUER =
            "spring.security.oauth2.resourceserver.jwt.issuer";
    private static final String PROPIEDAD_AUDIENCIAS =
            "spring.security.oauth2.resourceserver.jwt.audiences";

    private final Environment environment;

    public JwtDecoderValidatorPostProcessor(Environment environment) {
        this.environment = environment;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (!(bean instanceof NimbusJwtDecoder decoder)) {
            return bean;
        }

        String issuer = environment.getProperty(PROPIEDAD_ISSUER);
        String audiencias = environment.getProperty(PROPIEDAD_AUDIENCIAS);
        if (sinValor(issuer) || sinValor(audiencias)) {
            return bean;
        }

        List<OAuth2TokenValidator<Jwt>> validadores = new ArrayList<>();
        validadores.add(new JwtTimestampValidator());
        validadores.add(new JwtIssuerValidator(issuer));
        Arrays.stream(audiencias.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .forEach(audiencia -> validadores.add(new JwtAudienceValidator(audiencia)));
        decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(validadores));
        return bean;
    }

    /** Un placeholder sin resolver significa que el contexto usa un decoder de prueba. */
    private boolean sinValor(String valor) {
        return !StringUtils.hasText(valor) || valor.contains("${");
    }
}
