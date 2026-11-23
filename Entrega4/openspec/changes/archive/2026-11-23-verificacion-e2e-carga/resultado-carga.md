# Resultado de la prueba de carga (EP-21 / ADR-0002)

> Documento suelto del change `verificacion-e2e-carga`.
> Ejecutada el **2026-11-23** contra el compose activo de `Entrega4`, con el
> gateway en `127.0.0.1:8080` y **dos instancias de `msvc-entrada`** registradas
> en Eureka. Herramienta: **Apache JMeter 5.6.3** (instalado con winget,
> `DEVCOM.JMeter`).

## Plan

- Plan: `prueba-carga.jmx` (este directorio).
- Datos crudos: `resultados-carga/resultados.jtl`.
- Informe: las métricas de abajo, en este documento (ADR-0002 pide guardar los
  resultados como artefacto de documento, no como código generado versionado).

El plan no contiene secretos: los tokens se inyectan por propiedades. El informe
HTML se regenera cuando se necesite con:

```powershell
& "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat" `
  -g resultados-carga/resultados.jtl -o resultados-carga/informe
```

```powershell
& "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat" `
  -n -t prueba-carga.jmx `
  -l resultados-carga/resultados.jtl `
  -e -o resultados-carga/informe `
  "-JclientTokens=<CSV local con columnas token,cliente>" `
  "-JorgToken=<JWT de ORGANIZADOR>" `
  -Jthreads=30 -Jramp=10
```

Estructura del plan:

1. Grupo `Setup - reactivar fila`: `PUT /api/organizador/filas-virtuales/eventos/{id}/activacion`
   con `maxPersonas=60` y `cupoEntradas=120`.
2. Grupo `CLIENTES - ingreso, compra y pago`: 30 hilos virtuales, ramp-up de
   10 s, una iteración por hilo. CSV Data Set Config reparte un `CLIENTE`
   distinto por hilo. Cada hilo ejecuta:
   - `POST /api/filas-virtuales/eventos/{id}/ingresos` (200);
   - `POST /api/compras` con 2 entradas del sector 1 y `Idempotency-Key`
     única por hilo (201), extrae `idCompra`;
   - `POST /api/compras/{idCompra}/pago` con otra `Idempotency-Key` y
     `medioDePago=TARJETA` (200, cuerpo contiene `PAGADA`).

El gateway deriva `X-User-Id` del claim `idUsuario` del JWT, por eso el plan
solo envía `Authorization: Bearer`. También envía `X-Correlation-Id` por hilo.

## Datos previos

- Lugar 1 `Teatro Carga` con sector 1 `Campo` (`NO_NUMERADO`, capacidad 1000).
- Evento 2 `Show Carga 2`, publicado, con precio 25,00 por entrada.
- Fila del evento 2 activa con `maxPersonas=60`, `cupoEntradas=120`.
- 30 usuarios `CLIENTE` (`carga2_001` … `carga2_030`), cada uno con su JWT.

## Métricas

| Transacción | Muestras | Min | Mediana | Media | p90 | p95 | p99 | Máx | Errores | TPS |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Setup: activar fila | 1 | 58 | 58 | 58 | 58 | 58 | 58 | 58 | 0 | 17,24 |
| 1. Ingreso a fila | 30 | 27 | 38 | 47,9 | 84 | 118 | 138 | 138 | 0 | 3,11 |
| 2. Compra (2 entradas) | 30 | 43 | 54 | 71,8 | 130 | 157 | 183 | 183 | 0 | 3,11 |
| 3. Pago de compra | 30 | 45 | 62 | 79,1 | 155 | 192 | 211 | 211 | 0 | 3,14 |
| **Total** | **91** | **27** | **55** | **66,2** | **120** | **147** | **211** | **211** | **0 (0 %)** | **9,18** |

Tiempos en milisegundos. Duración total: ~10 s (30 hilos, ramp-up de 10 s).

## Integridad (anti doble venta)

Consultas sobre `ticketya_entrada` después de la corrida:

| Comprobación | Resultado |
|---|---:|
| Compras del evento 2 | 30, todas `PAGADA` |
| Usuarios distintos con compra | 30 |
| Claves de idempotencia de compra distintas | 30 |
| Pagos del evento 2 | 30, todos `APROBADO` |
| Claves de idempotencia de pago distintas | 30 |
| Entradas del evento 2 | 60, todas `VALIDA` |
| Intentos rechazados (`intento_compra`) | 0 |

Es decir: 30 clientes × 2 entradas = 60 entradas emitidas, sin compras ni
pagos duplicados y sin rechazos por cupo.

## Conclusión

- El flujo completo fila → compra → pago respondió **sin errores** con 30
  usuarios concurrentes y dos réplicas de `msvc-entrada`.
- El peor tiempo observado fue 211 ms (pago, p99); la latencia media global
  fue 66,2 ms. No aparecieron errores de convergencia durante la prueba.
- **Ajuste necesario:** en una corrida diagnóstica con `maxPersonas=30` —igual
  a la concurrencia— la tanda se cerró al llenarse y 29 compras recibieron 409
  `No hay un turno vigente`, porque el servicio solo acepta compras del turno
  de la tanda abierta. Con `maxPersonas=60` (mayor que la concurrencia) la
  tanda permaneció vigente y las 30 compras pasaron. Regla operativa: dimensionar
  `maxPersonas` por encima del número de clientes concurrentes de la prueba.
- La prueba corrobora la regla de no doble venta de EP-13/ADR-0002: unicidad e
  idempotencia funcionaron con varias instancias y sin base compartida entre
  servicios.

## Referencias

- `verificacion-despliegue-docker.md` (verificación manual y consumo).
- `docs/decisiones/0002-estrategia-pruebas-e2.md` (ADR de JMeter + compose).
- `docs/api-fila-virtual.md` (contrato de compra y pago).
