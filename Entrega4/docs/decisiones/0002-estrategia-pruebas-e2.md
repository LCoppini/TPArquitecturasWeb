# ADR-0002: Estrategia de pruebas de integracion y carga en Entrega 2

- Estado: propuesta
- Fecha: 2026-11-19

## Contexto

El `openspec/config.yaml` define que en Entrega 2:

- Las pruebas de integración entre microservicios se hacen levantando el
  entorno con `docker-compose`, no con Testcontainers.
- Se agregan pruebas que verifiquen que cada endpoint solo es accesible por los
  roles correspondientes.
- Se agrega una prueba de carga del flujo de compra con un informe de
  resultados: usuarios concurrentes, tiempos de respuesta y errores.
- La prueba de carga se ejecuta con al menos 2 instancias de `msvc-entrada`.

Hoy existe `msvc-entrada/src/test/java/.../integration/NoDobleVentaTest.java`,
que usa Testcontainers con `PostgreSQLContainer`. Los demás módulos tienen
paquetes `integration` todavía vacíos.

El usuario pidió expresamente **no escribir ni ejecutar tests en este paso**;
este ADR solo registra la decisión de estrategia.

## Decision

- Usar **`docker-compose` como entorno de integración**. Las pruebas de
  integración se ejecutan contra el sistema levantado con el
  `docker-compose.yml` del repositorio (config, Eureka, bases, servicios y
  gateway).
- No incorporar Testcontainers a las nuevas pruebas de integración. La prueba
  existente `NoDobleVentaTest` se revisa en su propio change; no se modifica
  para que pase.
- Agregar una **matriz de acceso por rol**: para cada endpoint protegido se
  verifica que responde solo para los roles autorizados y que los demás
  reciben 401 o 403. Estas pruebas atraviesan el gateway con tokens RS256.
- Agregar una **prueba de carga del flujo de compra** con JMeter, ejecutada con
  al menos 2 instancias de `msvc-entrada`. El informe debe incluir usuarios
  concurrentes, tiempos de respuesta y errores, y debe verificar la regla de no
  doble venta.
- Los resultados de carga se guardan como artefacto de documento (informe), no
  como secreto ni como código generado automáticamente versionado sin revisión.

## Alternativas consideradas

1. **Testcontainers.** Descartada por decisión del grupo: exige Docker desde el
   propio test y no valida el sistema completo con el compose real, que es lo
   que se va a presentar.
2. **Gatling o k6 para la carga.** Descartada: JMeter es la herramienta elegida
   y permite generar el informe solicitado sin nueva dependencia de runtime.
3. **Solo tests unitarios y de slice.** Descartada: no cubren ruteo, security
   chain ni concurrencia entre instancias.

## Consecuencias

### Mejoras

- Las pruebas reflejan el entorno real de entrega y de presentación.
- Se valida de punta a punta la cadena JWT → rol → header → servicio.
- La prueba de carga con 2 instancias ejerce la restricción de unicidad y la
  idempotencia del flujo de compra.

### Costos y riesgos

- Necesita Docker disponible para ejecutar la suite de integración.
- Los tests dependen de que el compose esté levantado y saludable; hay que
  documentar orden de arranque y timeouts.
- La prueba de carga requiere tokens válidos y un proveedor de tokens definido
  (dependencia del ADR-0001).
- `NoDobleVentaTest` queda como deuda a revisar: no se elimina ni se edita en
  este paso.

## Referencias

- `openspec/config.yaml`, sección "Pruebas"
- `openspec/changes/gateway-jwt-e2/specs/gateway-seguridad/spec.md`
- `docker-compose.yml`
