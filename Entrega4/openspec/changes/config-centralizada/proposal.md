# Proposal

## Why

Los microservicios de TicketYa necesitan valores comunes y coherentes para sus puertos, conexiones y descubrimiento de servicios. `msvc-config` ya sirve archivos YAML desde el classpath, pero su uso debe formar parte del arranque normal y los clientes necesitan valores locales equivalentes para tolerar que el servidor no esté disponible.

## What Changes

- Definir como fuente central los archivos empaquetados en `msvc-config`, usando el perfil `native`.
- Asegurar que los seis clientes (`msvc-eureka`, `msvc-gateway` y los cuatro servicios de dominio) importen `optional:configserver:http://localhost:8888` desde sus `application.yml` locales.
- Mantener en cada cliente valores locales suficientes para arrancar si el Config Server no está disponible.
- Exponer y verificar la configuración predeterminada de los seis componentes mediante `/{nombre-aplicacion}/default`.

**Fuera de alcance:** configuración externa por ambiente, perfiles dev/staging/prod, gestión de secretos, JWT y sus valores asociados, cambios de negocio y reintentos o refresco de configuración en caliente.

## Capabilities

### New Capabilities
- `config-centralizada`: publicación de configuración empaquetada y consumo con valores locales de respaldo.

### Modified Capabilities
- Ninguna.

## Impact

- **Servidor:** `msvc-config` y sus YAML en `src/main/resources/configuration`.
- **Clientes:** los `application.yml` locales de `msvc-eureka`, `msvc-gateway`, `msvc-usuarios`, `msvc-ubicacion`, `msvc-evento` y `msvc-entrada`.
- **Pruebas:** prueba de los seis endpoints del Config Server.
- **Documentación:** decisión de tolerancia al fallo y su costo de mantener duplicados.
