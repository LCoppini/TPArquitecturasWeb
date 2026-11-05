# AGENTS.md (constitucion del proyecto)

## Stack
Java 21 - Spring Boot 3.5.6 - Spring Cloud 2025.0.0 (Config, Eureka, Gateway MVC, OpenFeign) - Maven multi-modulo - PostgreSQL 16 - Lombok - MapStruct - springdoc-openapi

## Comandos
- Bases de datos: `docker compose up -d`
- Compilar todo: `mvn clean package`
- Orden de arranque: msvc-config (8888) -> msvc-eureka (8761) -> servicios de dominio -> msvc-gateway (8080)
- Tests de un servicio: `mvn -pl msvc-entrada test`

## Arquitectura
- Microservicios independientes: msvc-usuarios (8081), msvc-ubicacion (8082), msvc-evento (8083), msvc-entrada (8084). Entrada incluye pagos.
- Cada servicio: base propia, SIN foreign keys entre servicios (se referencian por id).
- Arquitectura en CAPAS dentro de cada servicio: `controller` -> `service` (+ `service/impl`) -> `repository` / `entity`. Las reglas de negocio van en `service` (y en las entidades cuando son propias de un solo objeto).
- El controller solo habla con el service; solo el service usa los repositories; no se exponen entidades en la API (DTOs en `dto/request` y `dto/response`).
- EDA (Entrega 2): eventos en `event`, publishers/listeners en `messaging`.
- Los clientes externos entran SOLO por msvc-gateway.

## Spec-driven development (OpenSpec)
- Todo cambio arranca con `/opsx:propose <nombre>`; no se codea sin proposal + specs + tasks revisados.
- Specs vivas en `openspec/specs/`, cambios en `openspec/changes/`. Criterios de aceptacion = casos de prueba.

## Principios
- Toda regla de negocio tiene su test.
- Anti doble venta: restriccion de unicidad en BD + idempotencia; debe funcionar con varias instancias.
- Nuevas dependencias o tecnologias solo con un ADR en `docs/decisiones/`.

## Nunca
- Modificar un test para que pase.
- Commitear secretos o `.env`.
- Exponer entidades JPA en la API.
- Acceder a la base de otro servicio.
