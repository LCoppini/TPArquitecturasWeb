# Verificación de seguridad y rutas

Este documento cierra la alineación de `gateway-jwt-e2`. La verificación sobre
`docker-compose` y JMeter queda fuera de alcance y se atiende en
`verificacion-e2e-carga` (EP-21).

## Cobertura de autenticación en `msvc-usuarios`

La cobertura ya estaba en el repositorio; no se modificaron tests para cerrar
este change. Se ejecutó con `mvn -B -pl msvc-usuarios test`.

| Requisito | Pruebas | Verificación |
|---|---|---|
| Login con email y BCrypt | `AutenticacionServiceImplTest` (5), `AutenticacionControllerTest` (3) | normalización del email, `PasswordEncoder.matches`, emisión solo con hash y roles, error genérico para password/email inválidos, usuarios legacy o sin roles, 401 genérico y 400 por validación. |
| Contrato RS256 del token | `TokenServiceImplTest` (2), `JwtEncoderConfigTest` (2) | claims `idUsuario`/`roles`/`iss`/`aud`/`exp`, expiración positiva, firma PKCS#8 verificable con la clave pública y rechazo de PEM que no es PKCS#8. |
| Registro público solo `CLIENTE` | `RegistroServiceImplTest` (3), `RegistroControllerTest` (3) | rol forzado a `CLIENTE`, password hasheado, email normalizado, conflictos por email/DNI, 201 público y 400/409. |
| Roles privilegiados | `AdminUsuarioServiceImplTest` (3), `AdministradorSeederTest` (3), `RolSeederTest` (1) | alta administrativa con roles solicitados y hash, rechazo de rol desconocido, seed por variables de entorno sin duplicados y catálogo de los cuatro roles. |
| Persistencia de usuarios y roles | `UsuarioPersistenciaIntegrationTest` (3) | catálogo idempotente, baja lógica que conserva asociaciones y unicidad de DNI. |

## Path único de artistas

| Capa | Consulta | Escritura | Estado |
|---|---|---|---|
| Configuración del gateway | `/api/artistas/**` → `msvc-evento` | `/api/admin/artistas/**` → `msvc-evento` | Igual en `msvc-gateway/src/main/resources/application.yml` y en `msvc-config/.../msvc-gateway.yml`. |
| Cadena de seguridad | `GET /api/artistas/**` en `permitAll` | `/api/admin/artistas/**` con `ADMINISTRADOR` | `SecurityConfig` y `GatewayRoleMatrix`. |
| Controllers de `msvc-evento` | `ParticipanteController` en `/api/artistas` (GET y GET `/{id}`) | `AdminParticipanteController` en `/api/admin/artistas` (POST y PUT `/{id}`) | No queda `@RequestMapping("/api/participantes")` en Java. |
| Feign | sin cliente de artistas | sin cliente de artistas | Ningún `@FeignClient` referencia `/api/artistas`, `/api/admin/artistas` ni `/api/participantes`. |
| Tests | `GatewayRoleMatrixTest.casosPublicos` | `GatewayRoleMatrixTest.casosDenegados` y `Administrador edita artistas` | La matriz falla si aparece una ruta del YAML sin regla explícita. |

Verificación reproducible:

```bash
git grep -n 'api/participantes' -- msvc-*/src/main/java msvc-config/src/main/resources docs
mvn -B -pl msvc-gateway,msvc-evento,msvc-entrada test
```

El primer comando no debe devolver rutas de implementación: la única forma
válida de consultar artistas es `/api/artistas`, y las escrituras viven bajo
`/api/admin/artistas`.

## Disponibilidad pública y contrato interno

- `GET /api/eventos/{idEvento}/disponibilidad?idSector=...` es público porque
  está dentro de `GET /api/eventos/**`.
- `EventoController` delega en `EventoServiceImpl`, que valida el evento y llama
  a `EntradaClient.consultarDisponibilidad`.
- `EntradaClient` usa `/internal/entradas/disponibilidad` (contexto propio, sin
  prefijo `/api/entradas`) y conserva las rutas `/api/entradas/eventos/...` para
  ventas y cancelación.
- `InternalDisponibilidadController` expone ese contrato en `msvc-entrada`; el
  gateway deniega `/internal/**`, así que no es accesible desde el exterior.
- La respuesta solo contiene `idEvento`, `idSector`, `capacidad`, `ocupadas` y
  `disponibles`, sin datos de compradores.

## Identidad en los servicios

`IdentityHeadersFilter` descarta el `X-User-Id` del cliente, lo sobrescribe con
el claim `idUsuario` del JWT y elimina `X-User-Role` de `getHeader`,
`getHeaders` y `getHeaderNames`. Los controllers de dominio leen únicamente
`X-User-Id`; la autorización por rol ocurre en la matriz del gateway.

Los `package-info` que describen este contrato quedaron alineados:

- `msvc-gateway/.../security/package-info.java` y
  `msvc-gateway/.../filter/package-info.java`.
- `msvc-evento/.../controller/package-info.java`,
  `msvc-evento/.../client/package-info.java` y
  `msvc-evento/.../entity/package-info.java`.
- `msvc-entrada/.../controller/package-info.java` declara explícitamente que no
  se recibe `X-User-Role`.

No se encontraron `package-info` con la propagación no autenticada de Entrega 1.
