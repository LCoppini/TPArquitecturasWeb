# Tasks

## 1. Persistencia y catálogo

- [x] 1.1 Modelar las entidades JPA `Usuario` y `Rol` y su asociación `usuario_rol`, incluyendo el estado activo y la unicidad de `dni`; verificar con pruebas de repositorio que las asociaciones se persisten y sobreviven a la baja lógica del usuario, el DNI duplicado se rechaza y las consultas distinguen usuarios activos.
- [x] 1.2 Inicializar de forma idempotente los cuatro roles acordados y sus restricciones de unicidad; verificar que la inicialización repetida no duplique registros y que los cuatro roles quedan disponibles.
- [x] 1.3 Implementar consultas de persistencia para listar y buscar usuarios activos por identificador y para comprobar DNI contra todos los registros; verificar resultados y comportamiento para identificadores inexistentes o inactivos.

## 2. Reglas de negocio

- [x] 2.1 Implementar los servicios de alta, consulta, listado, modificación y baja lógica de usuarios; verificar con pruebas unitarias altas, actualizaciones, conservación de registros y rechazo de DNI duplicado.
- [x] 2.2 Implementar consulta del catálogo y reemplazo idempotente de roles de usuario, incluida la remoción de todos los roles; verificar asignaciones múltiples, rol inválido, usuario inactivo y repetición de la misma solicitud.
- [x] 2.3 Traducir errores de negocio y persistencia a errores HTTP uniformes (400, 404 y 409); verificar que un conflicto de unicidad de base de datos también responde 409 sin ocultar otros errores.

## 3. API REST y contrato

- [x] 3.1 Crear DTOs de solicitud y respuesta validados y mapeos sin exponer entidades JPA; verificar campos obligatorios, DNI positivo y que la respuesta de usuario incluye `idUsuario` y roles.
- [x] 3.2 Implementar endpoints REST CRUD de usuarios, listado de roles y reemplazo de roles de usuario; verificar códigos HTTP, respuestas y casos de error mediante pruebas de controller.
- [x] 3.3 Documentar rutas, parámetros, cuerpos, respuestas y errores con OpenAPI/Swagger, y actualizar los `package-info.java` que describen rutas o el modelo; verificar que la documentación expone los contratos implementados.
- [x] 3.4 Verificar la compatibilidad de `GET /api/usuarios/{id}` con el DTO consumido por `msvc-entrada`; ejecutar las pruebas pertinentes de ambos módulos y comprobar que el cliente Feign puede deserializar `idUsuario`.

## 4. Integración

- [x] 4.1 Ejecutar las pruebas del módulo `msvc-usuarios` y compilarlo desde el reactor Maven; verificar que pasan las pruebas y que el módulo compila.
- [x] 4.2 Verificar contra PostgreSQL que el esquema actualizado permite inicializaciones repetidas y conserva usuarios y asociaciones al aplicar la baja lógica; registrar los pasos de ejecución necesarios en la documentación del servicio.
