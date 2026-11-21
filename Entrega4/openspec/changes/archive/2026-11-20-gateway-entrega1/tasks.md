# Tareas

> Cierre histórico del 2026-11-20: `gateway-jwt-e2` supersede la propagación de
> headers de Entrega 1. Las tres tareas administrativas que no dejaron registro
> versionado se cierran como absorbidas o supersedidas, sin afirmar work que no
> está documentado. La verificación vigente está en `gateway-jwt-e2` y en
> `docs/verificacion-seguridad-rutas.md`.

- [x] Revisar propuesta, rutas y contrato de errores con el grupo antes de
      implementar. Absorbida por la revisión de `gateway-jwt-e2`; el contrato
      uniforme de 404/503 se conserva.
- [x] Confirmar que la lista de paths coincide con los controladores.
- [x] Comparar la lista final de paths con la colección de Postman del grupo.
      Supersedida: no hay colección versionada; `GatewayRoleMatrixTest` falla si
      aparece una ruta del YAML sin regla explícita.
- [x] Documentar las rutas de Entrega 1 en la configuración central, con
      comentarios concisos.
- [x] Implementar el formato JSON uniforme para 404 y 503 sin alterar errores
      provenientes de los servicios destino.
- [x] Agregar pruebas para ruteo, propagación de headers y respuestas 404/503.
- [x] Probar que `/api/registros-pago/**` no tiene ruta pública.
- [x] Ejecutar las pruebas del gateway: 4 tests ejecutados, 0 fallos y 0 errores.
- [x] Ejecutar un smoke test por cada servicio
      registrado en Eureka. Supersedido por el smoke test punta a punta
      documentado en `gateway-jwt-e2/tasks.md`; la matriz por rol sobre el
      entorno real queda en `verificacion-e2e-carga` (EP-21).
- [x] Actualizar README; el contrato de las APIs de dominio no cambió.
