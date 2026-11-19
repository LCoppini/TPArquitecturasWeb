# msvc-preferencias

**Estado: esqueleto (Entrega 2).** Tiene la estructura de capas, el `pom.xml`, la configuración y un `package-info.java` por paquete con lo previsto. Todavía no tiene lógica.

## Responsabilidad

- Registrar automáticamente las preferencias de cada cliente (tipos de evento y artistas) a partir de su historial de compras: cada vez que msvc-entrada emite una entrada, se registra el tipo y los artistas del evento.
- Recomendar eventos del mismo tipo y/o con los mismos artistas favoritos. Versión simplificada, sin "quienes compraron esto también compraron"; justificarlo en `docs/decisiones`.
- Informar a msvc-notificaciones qué clientes tienen a un artista como favorito.

## Datos

- Puerto: `8086`.
- Base: **MongoDB** `ticketya_preferencias`, un documento por cliente (`id` = `id_usuario`). Se usa MongoDB porque el documento es anidado, de esquema flexible y se lee por id. El paquete se llama `entity` para mantener la misma estructura que los demás servicios.

## Ejecución local

1. Desde la raíz de `Entrega4`: `docker compose up -d` (incluye el contenedor `mongo`).
2. Iniciar `msvc-config` y `msvc-eureka`, y luego este servicio (`mvn -pl msvc-preferencias spring-boot:run`). No necesita PostgreSQL.
3. Swagger UI: `http://localhost:8086/swagger-ui.html`.

## Gateway

Rutas públicas: `/api/preferencias/**` y `/api/recomendaciones/**`. Los endpoints internos (registro de compras, clientes por artista) se restringen en el gateway al implementar JWT.

## Puntos abiertos

- Redis para cache o fila virtual: en discusión; no se agregó ninguna dependencia.
- Mecanismo asincrónico para recibir `EntradaEmitida` (broker o llamada REST): lo decide el grupo.
- ADR de MongoDB pendiente en `docs/decisiones` (usar `0000-plantilla.md`).
