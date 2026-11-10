# Propuesta: completar el gateway de Entrega 1

## Por qué

`msvc-gateway` ya enruta las APIs principales mediante Eureka, pero aún no
define un contrato uniforme de errores para rutas inexistentes y servicios
destino no disponibles. También conviene dejar explícito el alcance de las
rutas para que coincida con los controladores realmente expuestos por los
microservicios.

## Qué cambia

- Mantener al gateway como punto de entrada HTTP único para las APIs públicas.
- Enrutar por Eureka las rutas de usuarios, roles, lugares, sectores, eventos,
  artistas, entradas y pagos.
- Enrutar `/api/entradas/**` a `msvc-entrada`; este path ya incluye solicitud,
  resolución y reintento de devoluciones.
- No crear una ruta para `/api/registros-pago`, ya que ese endpoint no forma
  parte de la API pública acordada.
- Propagar los headers `X-User-Id` y `X-User-Role` recibidos, sin afirmar que
  sean identidad autenticada.
- Devolver errores con una estructura JSON uniforme para rutas inexistentes y
  servicios no disponibles.

## Capacidades

- Nueva capacidad: `gateway-ruteo` — rutas públicas y respuestas uniformes del
  gateway en Entrega 1.

## Alcance

- Configuración centralizada del gateway en
  `msvc-config/src/main/resources/configuration/msvc-gateway.yml`.
- Manejo uniforme de errores HTTP del gateway.
- Pruebas de rutas/errores que puedan ejecutarse sin levantar manualmente
  microservicios.
- Comentarios breves en la configuración para explicar el destino y alcance de
  cada grupo de rutas.

## Fuera del alcance

- JWT, autenticación real y autorización por rol (Entrega 2, EP-12).
- Circuit breaker, reintentos y fallbacks (Entrega 2, EP-13).
- Trazabilidad centralizada (Entrega 2, EP-20).
- Una ruta pública `/api/registros-pago`.
- Una ruta separada `/api/devoluciones`: las operaciones de devolución se
  mantienen bajo `/api/entradas/{id}/devolucion`.
- Reglas de negocio de los servicios de dominio.

## Impacto

Los clientes externos usarán el puerto `8080`. El gateway localizará cada
servicio usando su nombre registrado en Eureka (`lb://...`), sin direcciones
fijas de los microservicios. No se modificarán los paths ni los DTOs de las
APIs de dominio.

Los errores uniformes normalizan la respuesta del gateway, pero no deben
reemplazar ni ocultar errores de negocio enviados por un servicio destino.
