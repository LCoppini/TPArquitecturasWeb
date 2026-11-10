# Tasks

## 1. Configuración de clientes

- [x] 1.1 Verificar que los seis clientes declaren `spring.config.import=optional:configserver:http://localhost:8888` en su `application.yml` local.
- [x] 1.2 Mantener valores locales suficientes y coherentes con la configuración central para arrancar sin Config Server.
- [x] 1.3 Confirmar que `msvc-config` conserva el perfil `native` y lee `classpath:/configuration`.

## 2. Contrato y pruebas

- [x] 2.1 Probar `/{nombre-aplicacion}/default` para `msvc-usuarios`, `msvc-ubicacion`, `msvc-evento`, `msvc-entrada`, `msvc-gateway` y `msvc-eureka`, validando el YAML esperado.
- [x] 2.2 Validar que la configuración local de cada cliente contiene la directiva de importación en la ruta correcta y los valores de respaldo documentados.
- [x] 2.3 Verificar en tiempo de ejecución, para al menos un cliente, que una propiedad definida tanto localmente como en `msvc-config` resuelve al valor central, y que una propiedad definida solo localmente se mantiene cuando `msvc-config` no la define.
