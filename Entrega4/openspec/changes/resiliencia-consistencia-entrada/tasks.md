# Tasks

## 1. Revisión y prerrequisitos

- [ ] 1.1 Revisar `proposal.md`, `specs/`, `design.md` y ADR-0004 con el grupo.
      Verificar con `openspec validate resiliencia-consistencia-entrada`.
- [ ] 1.2 Confirmar el prerrequisito ya cerrado: `IdentityHeadersFilter` elimina
      `X-User-Role`, `EntradaClient` solo envía `X-User-Id` en cancelación y no
      hay parámetro `rol` en los clients de `msvc-entrada`.

## 2. Configuración central (valores concretos)

- [ ] 2.1 En `msvc-config/src/main/resources/configuration/msvc-entrada.yml` y
      `msvc-evento.yml` definir: Feign `connect: 200 ms`, `read: 800 ms`;
      `TimeLimiter: 1 s`; retry `2 intentos` con backoff `100 ms`;
      CircuitBreaker `TIME_BASED`, ventana `120 s`, mínimo `3` llamadas, umbral
      `50 %`, `waitDurationInOpenState: 30 s`, half-open `3` llamadas y transición
      automática.
- [ ] 2.2 Definir `ticketya.reserva.ttl-minutos: 10` y el backoff de reemisión
      `100/200/400/800 ms` para 5 intentos.
- [ ] 2.3 Agregar un test de binding que verifique exactamente esos valores.

## 3. Validación previa y latencia

- [ ] 3.1 Ordenar la saga: validar evento, usuario y sector antes de reservar.
      Con fail-closed, si falla la validación no se crea la reserva.
- [ ] 3.2 Aplicar retry solo a lecturas idempotentes; excluir 4xx del retry y del
      cómputo de falla del circuito; ignorar `CallNotPermittedException`.
- [ ] 3.3 Casos de test: `EventoClient` lento, `UsuarioClient` caído,
      `SectorClient` con red/5xx, 4xx sin retry, circuito abierto con 502 y
      recuperación half-open → closed.
- [ ] 3.4 Verificar la latencia peor caso de 6,3 s con 2 intentos, frente a 9,9 s
      con 3.

## 4. Idempotencia y carrera TTL

- [ ] 4.1 Agregar `UNIQUE (id_entrada, clave_idempotencia)` en base de datos.
- [ ] 4.2 Misma clave e igual huella devuelve el resultado original sin cobrar;
      distinta huella responde 409 en lugar del 422 actual.
- [ ] 4.3 Confirmar la reserva con `UPDATE ... WHERE estado = 'RESERVADA' AND
      expiraEn > now()`. Si afecta 0 filas, reintegrar y no emitir.
- [ ] 4.4 Casos de test: dos requests concurrentes con la misma clave, carrera
      TTL/pago aprobado, pago `FALLA` con nuevo intento usando otra clave y otra
      reserva. Estos corren contra la base PostgreSQL de `docker compose up -d`,
      sin Testcontainers nuevos.

## 5. Emisión pendiente

- [ ] 5.1 Incorporar el estado `PAGO_APROBADO_SIN_EMITIR` y el worker que lo
      retoma.
- [ ] 5.2 Reemitir hasta 5 intentos con backoff `100/200/400/800 ms`; si se
      agotan, reintegrar con la clave del pago.
- [ ] 5.3 Casos de test: emisión correcta sin QR duplicado y agotamiento de
      intentos con reintegro.

## 6. TTL con dos instancias

- [ ] 6.1 Barrido de reservas vencidas por `UPDATE` condicional, idempotente.
- [ ] 6.2 Verificar con dos instancias de `msvc-entrada` sobre `docker-compose`
      en EP-21 que cada fila se procesa una sola vez y que una reserva aprobada
      nunca se libera.

## 7. Clients y fallas aisladas

- [ ] 7.1 Aplicar timeout y circuit breaker a `EventoClient`, `UsuarioClient`,
      `SectorClient` y `EntradaClient.consultarReporte`.
- [ ] 7.2 En `EntradaClient.cancelarEntradas` aplicar solo timeout y circuito;
      sin retry automático de Feign. El reintento lo hace
      `reintentarCoordinacionCancelacion`.
- [ ] 7.3 Casos de test: notificación caída y preferencias caídas dejan la compra
      `APROBADA`/`VALIDA`.
- [ ] 7.4 Verificar que no se modificaron `NoDobleVentaTest` ni
      `QrAccesoConcurrenciaTest`.

## 8. Verificación y cierre

- [ ] 8.1 Ejecutar `mvn -B -pl msvc-entrada,msvc-evento test` y luego
      `mvn -B package`.
- [ ] 8.2 Ejecutar `openspec validate resiliencia-consistencia-entrada`.
- [ ] 8.3 ADR-0004 queda en borrador, pendiente de aprobación del grupo.
- [ ] 8.4 Archivar moviendo la carpeta a `openspec/changes/archive/`, sin agregar
      `archived:` a `.openspec.yaml`, igual que el change de reportes.
