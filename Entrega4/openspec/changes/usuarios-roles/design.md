# Design

## Context

`msvc-usuarios` ya tiene paquetes vacíos para controller, service, repository, entity y DTO, y depende de Spring Web, Validation y Data JPA. Usa PostgreSQL propio (`ticketya_usuarios`) y la configuración central define `ddl-auto: update`. No hay capacidades publicadas en `openspec/specs/`. `msvc-entrada` consume `GET /api/usuarios/{id}` por Feign y deserializa el identificador como `idUsuario` (`id` e `id_usuario` también son alias aceptados). El modelo define usuario, rol y la tabla de asociación `usuario_rol`.

## Goals / Non-Goals

**Goals:**
- Implementar las capacidades descritas en `specs/usuarios/spec.md` y `specs/roles-usuario/spec.md` respetando las capas del servicio.
- Mantener la consulta por ID compatible con el consumidor Feign existente.
- Persistir asociaciones y catálogo de roles sin duplicar roles durante inicializaciones repetidas.

**Non-Goals:**
- Autenticación, credenciales, JWT o autorización por rol en Entrega 1.
- CRUD del catálogo de roles, autorización de acciones de negocio o integración de QR.
- Cambios a `msvc-entrada` salvo que las pruebas del contrato revelen una incompatibilidad que no pueda resolverse desde la respuesta del servicio.

## Decisions

1. **Mantener PostgreSQL y JPA existentes.** Se utilizarán las dependencias actuales, con `usuario` como entidad raíz, `rol` como catálogo y `usuario_rol` como asociación muchos-a-muchos. No se agrega una tecnología ni dependencia nueva. Alternativa descartada: almacenar roles como texto en `usuario`, porque contradice el modelo y dificulta mantener un catálogo consistente.

2. **Baja lógica con estado activo.** Se agrega un indicador `activo` con valor inicial verdadero. Las consultas de listado, modificación y consulta por ID operan sobre usuarios activos; la baja conserva el registro y las relaciones históricas. Un DNI debe ser único entre todos los registros, también los inactivos. La unicidad se valida en negocio y se respalda con una restricción de base de datos. Alternativa descartada: borrado físico, porque elimina datos y asociaciones que pueden seguir referenciados por entradas.

3. **Catálogo sembrado de forma idempotente.** Los cuatro registros (`CLIENTE`, `ORGANIZADOR`, `ADMINISTRADOR`, `PERSONAL_ACCESO`) se inicializan en PostgreSQL y `rol.tipo` es único. La inicialización no debe depender de IDs numéricos fijos y debe poder repetirse sin duplicar filas. Alternativa descartada: representar roles solo como enum sin filas persistidas, porque `usuario_rol` necesita asociarlos en la base.

4. **API de roles de solo lectura y asignación por reemplazo.** `GET /api/roles` expone el catálogo. `PUT /api/usuarios/{id}/roles` recibe los nombres válidos y reemplaza el conjunto existente; una lista vacía quita todas las asociaciones. No se ofrecen endpoints para escribir el catálogo. Alternativa descartada: endpoints de asignación y remoción separados, que harían ambiguo el estado final ante reintentos.

5. **DTOs separados y errores HTTP explícitos.** Las solicitudes y respuestas usan DTOs validados; las entidades JPA no se exponen. El handler global traduce validaciones a 400, recursos inexistentes/inactivos a 404 y DNI duplicado a 409. `GET /api/usuarios/{id}` incluye `idUsuario` y mantiene los datos del usuario y roles para el contrato interservicio.

6. **Sin autenticación en Entrega 1.** Las rutas se exponen a través del gateway según la configuración ya existente, sin exigir JWT ni interpretar roles como permisos. Alternativa descartada: incorporar Spring Security ahora, porque la consigna reserva JWT y autorización para Entrega 2.

## Risks / Trade-offs

- **Actualización automática del esquema:** Hibernate `update` no constituye una estrategia de migración versionada. Se mantiene la configuración existente para este cambio; antes de una entrega con datos persistentes, debe verificarse el efecto de añadir `activo` y unicidad de DNI, y acordar una estrategia de migración.
- **Duplicados existentes de DNI:** una restricción única no podrá aplicarse si hay duplicados previos. Antes de activar la restricción, se debe comprobar la base y resolver duplicados sin descartar datos.
- **Alta simultánea con DNI repetido:** la validación previa puede competir entre instancias; la restricción única de PostgreSQL es la autoridad final y el servicio debe convertir su violación en 409.
- **Reemplazo concurrente de roles:** si llegan solicitudes `PUT /api/usuarios/{id}/roles` concurrentes para el mismo usuario, el resultado esperado es *last write wins* según el orden efectivo de persistencia, sin garantía de orden entre solicitudes.
- **Roles no son permisos en E1:** la presencia del rol `PERSONAL_ACCESO` no implica autorización para validar QR, que queda fuera de esta entrega.
