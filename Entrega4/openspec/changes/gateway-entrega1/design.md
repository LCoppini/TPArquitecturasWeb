# Diseño: gateway de Entrega 1

## Rutas públicas

| Path | Servicio |
|---|---|
| `/api/usuarios/**` | `msvc-usuarios` |
| `/api/roles/**` | `msvc-usuarios` |
| `/api/lugares/**` | `msvc-ubicacion` |
| `/api/sectores/**` | `msvc-ubicacion` |
| `/api/eventos/**` | `msvc-evento` |
| `/api/artistas/**` | `msvc-evento` |
| `/api/entradas/**` | `msvc-entrada` |
| `/api/pagos/**` | `msvc-entrada` |

Cada ruta usa `lb://<service-id>` para resolver instancias mediante Eureka.
No se agrega `/api/registros-pago`; tampoco se separan devoluciones porque ya
están incluidas dentro de `/api/entradas/**`.

## Headers de usuario

En Entrega 1, el gateway deja pasar `X-User-Id` y `X-User-Role` que formen parte
de la solicitud. La propagación no valida ni autentica esos valores. Por eso
esta configuración solo corresponde al entorno de la entrega sin JWT; los
servicios no deben quedar expuestos como APIs públicas fuera del gateway.

## Errores

- Path sin ruta configurada: respuesta 404 en el formato JSON acordado.
- Ruta configurada sin instancia destino disponible: respuesta 503 en el mismo
  formato.
- Errores HTTP de negocio devueltos por el microservicio destino: se conservan,
  sin reemplazar su status ni su body.

El formato concreto propuesto es `status`, `error`, `message` y `path`. El
mensaje no incluirá stack traces ni detalles internos de infraestructura.

## Validación

- Verificar cada ruta con un servicio destino registrado en Eureka.
- Verificar 404 para un path no configurado.
- Verificar 503 cuando la ruta existe pero no hay una instancia disponible.
- Verificar que se conservan status y body de una respuesta de error del destino.
- Comprobar que Swagger y los puertos de los servicios siguen funcionando
  conforme al README.
