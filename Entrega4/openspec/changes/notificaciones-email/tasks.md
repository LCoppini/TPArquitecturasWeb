# Tasks

## 1. Persistencia y contrato

- [x] 1.1 Crear `Notificacion`, `TipoNotificacion` y `EstadoNotificacion` con `UNIQUE(clave_idempotencia)` y `NotificacionRepository` con las consultas del historial, el reclamo atómico y la búsqueda de elegibles; verificar al arrancar el servicio que `ddl-auto` crea la tabla `notificacion` con la restricción de unicidad.
- [x] 1.2 Crear `SolicitudNotificacionRequest`, `NotificacionResponse` y `NotificacionMapper`; verificar con `SolicitudNotificacionRequestValidacionTest` que la clave, el usuario y el tipo son obligatorios y que el destinatario debe ser un email válido.

## 2. Solicitud idempotente

- [x] 2.1 Implementar `PlantillaEmail` con las cuatro plantillas y los datos obligatorios por tipo; verificar con `PlantillaEmailTest` los faltantes, el saludo con y sin nombre y que no quedan marcadores sin reemplazar.
- [x] 2.2 Implementar `NotificacionService.solicitar` (idempotencia por clave, carrera por clave duplicada, pool saturado); verificar con `NotificacionServiceImplTest` solicitud nueva, repetida, simultánea, con datos faltantes y con el pool rechazando la tarea.
- [x] 2.3 Crear `NotificacionController` bajo `/internal/notificaciones` con `NotificacionesExceptionHandler`; verificar con `NotificacionControllerTest` los códigos 202, 400 y 404 y el formato de error.

## 3. Envío asíncrono y reintentos

- [x] 3.1 Configurar `@EnableAsync`, el `ThreadPoolTaskExecutor` propio, `MdcTaskDecorator` y `NotificacionesProperties`; verificar con `MdcTaskDecoratorTest` que el hilo del pool conserva el `correlationId`.
- [x] 3.2 Implementar `EnviadorEmailSmtp` con `JavaMailSender`; verificar con `EnviadorEmailSmtpTest` el remitente, el destinatario, el asunto, el cuerpo y la traducción de errores SMTP.
- [x] 3.3 Implementar `NotificacionEnvioServiceImpl` (reclamo atómico, destinatario, éxito, fallo con backoff, intentos agotados, circuito abierto y descarte); verificar con `NotificacionEnvioServiceImplTest` cada transición de estado y el MDC durante el envío.
- [x] 3.4 Implementar `NotificacionReintentoWorker`; verificar con `NotificacionReintentoWorkerTest` que envía cada elegible por el proxy de `@Async` y corta la pasada si el pool se satura.

## 4. Resiliencia

- [ ] 4.1 Implementar `EnviadorEmailResiliente` (Circuit Breaker + TimeLimiter, instancia `email`) y configurar `resilience4j.*` y los timeouts SMTP y Feign; verificar con credenciales SMTP inválidas que, tras las llamadas mínimas del circuito, las notificaciones vuelven a `PENDIENTE` con `intentos` sin cambios y próximo intento posterior a 35 segundos.

## 5. Trazabilidad

- [ ] 5.1 Crear `CorrelationIdFilter` y el patrón de log con `%X{correlationId}`; verificar con `CorrelationIdFilterTest` el header recibido, el generado y la limpieza del MDC, y revisar en el log que el envío muestra el mismo `correlationId` de la solicitud.

## 6. Configuración, documentación y colección

- [x] 6.1 Agregar SMTP, Feign, `ticketya.notificaciones.*` y `resilience4j.*` a `msvc-config/.../msvc-notificaciones.yml` y al `application.yml` local, y actualizar README y `package-info.java` del servicio y el README raíz; verificar que ambos YAML cargan y que el README explica las variables `MAIL_*`.
- [ ] 6.2 Agregar `postman/TicketYa-Notificaciones.postman_collection.json`; verificar con el Runner que los siete requests pasan con el servicio levantado.

## 7. Verificación y cierre

- [x] 7.1 Ejecutar `mvn -pl msvc-notificaciones test` y verificar que compilan y pasan los tests.
- [x] 7.2 Levantar el entorno con un inbox de Mailtrap, enviar una solicitud y verificar el email recibido, el estado `ENVIADA` por `GET /internal/notificaciones/{id}` y que repetir la clave no envía un segundo email.
- [ ] 7.3 Levantar dos instancias de `msvc-notificaciones` en puertos distintos, crear varias notificaciones y verificar que cada una se envía una sola vez.
- [ ] 7.4 Verificar el alcance: `git status` solo muestra cambios en `msvc-notificaciones`, el YAML de `msvc-config`, el README raíz, `postman/` y `openspec/changes/notificaciones-email`; no se agregó RabbitMQ, Redis ni Caffeine ni un ADR nuevo; no se tocó `msvc-entrada`, `msvc-evento`, `msvc-usuarios` ni `msvc-gateway`; y `openspec validate "notificaciones-email"` termina sin errores.
