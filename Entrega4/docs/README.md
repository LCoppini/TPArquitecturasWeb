# Documentacion de diseno

| Carpeta / archivo | Contenido |
|---|---|
| `user-stories.md` | Features y user stories con criterios de aceptacion (migrar desde Trello) |
| `diagramas/` | Modulos, secuencia de compra (con pago rechazado y reserva vencida), ER por microservicio |
| `api/` | Contratos OpenAPI exportados de cada servicio |
| `eventos.md` | Catalogo de eventos de dominio (EDA) y claves de deduplicación de `CompraConfirmada`/`EntradaEmitida` |
| `api-fila-virtual.md` | Contrato HTTP de fila virtual y Compra: rutas, DTOs, códigos, idempotencia, vencimiento y devolución |
| `operacion-fila-virtual.md` | Procedimiento operativo de desactivación, drenaje y rollback de la fila virtual |
| `verificacion-seguridad-rutas.md` | Cobertura de login/registro, path único de artistas y contrato de identidad del cierre `gateway-jwt-e2` |
| `verificacion-fila-virtual.md` | Comandos, resultados, evidencia y bloqueos de la verificación del change `fila-virtual` |
| `decisiones/` | ADRs: una decision tecnica por archivo (usar `0000-plantilla.md`) |
