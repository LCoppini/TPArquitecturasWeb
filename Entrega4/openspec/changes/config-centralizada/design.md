# Design

## Decisions

1. **Configuración empaquetada en classpath.** Se conserva el perfil `native` y `classpath:/configuration`; no se agrega una fuente externa porque Entrega 1 no contempla múltiples ambientes.
2. **Import opcional con uso normal.** Los seis clientes importan Config Server mediante `optional:configserver:` al iniciar. `optional` tolera su indisponibilidad, pero no sustituye el uso central cuando el servidor está activo.
3. **Respaldo local duplicado.** Los clientes mantienen en su `application.yml` local los valores necesarios para arrancar sin el servidor. Esto evita que una interrupción de Config Server impida iniciar, a costa de mantener alineadas dos copias de la configuración.
4. **Sin secretos.** Gestión de secretos, `jwt.secret` y `jwt.expiration` siguen fuera de alcance en Entrega 1; se reservan para Entrega 2.

## Risks / Trade-offs

- **Desalineación entre valores locales y centrales:** al duplicar valores de respaldo en cada `application.yml` local y en los YAML servidos por `msvc-config`, ambas copias pueden divergir si se actualiza una sola. Cualquier cambio de configuración debe reflejarse en ambos lugares, o deben revisarse periódicamente para confirmar que coinciden.
- **Uso de respaldo local durante una caída:** `optional:configserver:` permite iniciar con los valores locales cuando `msvc-config` no está disponible, pero esos valores podrían estar desactualizados y la importación opcional no proporciona por sí sola una estrategia de reintento o refresco. Debe monitorearse la disponibilidad del servidor y, tras una caída, asegurarse de que los clientes vuelvan a cargar la configuración central según el procedimiento operativo.
- **Sin configuración por ambiente en esta entrega:** los YAML empaquetados sirven valores predeterminados y no distinguen entre dev, staging o prod. Diferencias específicas por ambiente deberán resolverse en una entrega posterior antes de desplegar allí, evitando introducirlas como cambios ad hoc en las copias locales.

## Runtime verification

Se verificó `msvc-usuarios` con PostgreSQL activo y `msvc-config` en el puerto 8888. Para la prueba se crearon temporalmente `ticketya.config-priority-probe` y `ticketya.local-only-probe`: con el Config Server disponible, `GET /actuator/env/ticketya.config-priority-probe` devolvió `central-usuarios` desde `configserver:classpath:/configuration/msvc-usuarios.yml`, mientras que `GET /actuator/env/ticketya.local-only-probe` devolvió `local-only-usuarios` desde el `application.yml` local. Luego se detuvo `msvc-config` y se reinició el cliente: este arrancó en el puerto 8081 y ambos endpoints devolvieron los valores locales (`local-usuarios` y `local-only-usuarios`). Tras confirmar el comportamiento, ambas propiedades temporales se retiraron de los YAML local y central; no están en la configuración actual. Actuator se expuso solo mediante argumentos de esa ejecución; se usó `-Duser.timezone=UTC` para la conexión local a PostgreSQL.
