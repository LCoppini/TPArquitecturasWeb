# Informe JMeter — verificación de carga tope de lote

> **Alcance explícito:** la carga se ejecutó **sin el flujo aviso-artista-favorito**.
> El plan `prueba-carga.jmx` cubre únicamente: activar fila (organizador),
> `POST` de ingreso a fila, polling `GET mi-estado` hasta `HABILITADO`,
> `POST` de compra de 2 entradas y `POST` de pago. No se ejecutaron avisos al
> artista, favoritos ni endpoints de preferencias/notificaciones. Estos
> resultados **no cubren** ese flujo.

- Fecha: 2026-11-23.
- Change: `corregir-tope-lote-fila`.
- Evento **21**, escenario `r45m30top`.
- JTL XML: `jtl/ramp0-45-tope.jtl` (250 muestras).
- Informe HTML generado desde el JTL: `informe-jmeter.html`.
- Resumen original de la corrida: `carga-verificacion.md`.

## Parámetros

| Parámetro | Valor |
|---|---|
| Clientes / ramp | 45 / 0 |
| `maxPersonas` / `cupoEntradas` | 30 / 60 por tanda |
| Entradas por compra | 2 |
| Timeout de polling | 60000 ms (`-JpollTimeoutMs=60000`) |
| Infraestructura | 2 réplicas de `msvc-entrada` + gateway en `127.0.0.1:8080` |
| Ventana medida (UTC-3) | 17:26:10.470 → 17:26:18.411 |
| Duración / throughput | 7,94 s / 31,48 muestras por segundo |

## Métricas por sampler

| Sampler | N | OK | Err | Min ms | Avg ms | p95 ms | Max ms |
|---|---:|---:|---:|---:|---:|---:|---:|
| 1 - POST ingreso a fila | 45 | 45 | 0 | 254 | 1307,0 | 2217 | 2335 |
| 2a - GET mi-estado | 114 | 114 | 0 | 13 | 491,2 | 1750 | 1883 |
| 3 - POST compra (2 entradas) | 45 | 45 | 0 | 137 | 935,4 | **1887** | 2364 |
| 4 - POST pago de compra | 45 | 45 | 0 | 320 | 1457,6 | 2291 | 2328 |
| PUT activar fila (setup) | 1 | 1 | 0 | 282 | 282,0 | 282 | 282 |
| **Global** | **250** | **250** | **0** | **13** | **891,1** | **2136** | **2364** |

Códigos de respuesta: `200` × 205, `201` × 45. Sin timeouts de polling ni fallos.

## Verificación funcional en base

| Comprobación | Valor |
|---|---:|
| Compras OK / pagadas | 45 / 45 |
| Entradas `VALIDA` | 90 |
| Sobreventa | 0 (90 ≤ 1000) |
| Tanda 1 / tanda 2 | 30 personas (seq 1–30) / 15 personas (seq 31–45) |
| Inversiones FIFO | 0 |
| Claves de idempotencia | 45 compra + 45 pago, todas distintas |

Los 15 excedentes quedaron `EN_ESPERA` y se habilitaron al cerrar la tanda 1.
`cupoEntradas` sigue siendo por tanda: la tanda 2 abrió con 60 de cupo.

## Fuentes y notas

- Plan JMeter: `../../archive/2026-11-23-turno-vigente-al-ingresar/jmeter/prueba-carga.jmx`.
- Los tokens se pasaron por propiedades (`-JclientTokens`, `-JorgToken`); no hay credenciales en el JTL ni en este informe.
- JMeter CLI no está disponible en el entorno local, por lo que el HTML se generó directamente desde el JTL XML sin agregar dependencias al proyecto.
- No se versionaron `jmeter.log` ni informes HTML previos; este `informe-jmeter.html` es el único informe HTML añadido.
