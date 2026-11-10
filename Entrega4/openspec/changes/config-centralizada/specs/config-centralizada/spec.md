# Spec Delta

## Purpose

Definir la publicación de configuración central de TicketYa y el comportamiento de sus clientes cuando `msvc-config` está disponible o no al iniciar.

## ADDED Requirements

### Requirement: Publicación de configuración centralizada (Entrega 1)
Cuando se consulta la configuración predeterminada de un componente servido, `msvc-config` deberá (SHALL) devolver el YAML empaquetado correspondiente a ese componente.

#### Scenario: Configuración predeterminada disponible
- **WHEN** se consulta `/{nombre-aplicacion}/default` para `msvc-usuarios`, `msvc-ubicacion`, `msvc-evento`, `msvc-entrada`, `msvc-gateway` o `msvc-eureka`
- **THEN** el endpoint responde 200 y expone la configuración correspondiente al nombre de aplicación, incluyendo el puerto y las propiedades específicas definidas para ese componente

### Requirement: Consumo normal del Config Server (Entrega 1)
Cuando un microservicio cliente inicia con `msvc-config` disponible, deberá (SHALL) importar la configuración central como parte de su configuración de arranque. Si una propiedad está definida tanto en el `application.yml` local como en la configuración central, prevalece el valor provisto por `msvc-config`; los valores locales sirven de respaldo cuando el servidor no está disponible o para propiedades que la configuración central no define.

#### Scenario: Config Server disponible
- **WHEN** inicia `msvc-eureka`, `msvc-gateway`, `msvc-usuarios`, `msvc-ubicacion`, `msvc-evento` o `msvc-entrada` mientras `msvc-config` está disponible
- **THEN** el cliente solicita su configuración central usando su nombre de aplicación y continúa el arranque con los valores servidos

#### Scenario: Propiedad definida localmente y en el Config Server
- **WHEN** inicia un cliente con `msvc-config` disponible y una misma propiedad está definida tanto en su `application.yml` local como en su configuración central
- **THEN** el cliente usa el valor provisto por `msvc-config`

#### Scenario: Propiedad definida solo en el Config Server
- **WHEN** inicia un cliente con `msvc-config` disponible y una propiedad existe en la configuración central pero no está definida en su `application.yml` local
- **THEN** el cliente usa el valor provisto por `msvc-config` para esa propiedad

#### Scenario: Propiedad definida solo localmente
- **WHEN** inicia un cliente con `msvc-config` disponible y una propiedad existe en su `application.yml` local pero no está definida en la configuración central
- **THEN** el cliente usa el valor local para esa propiedad, sin que la ausencia en `msvc-config` impida el arranque

### Requirement: Valores locales de respaldo (Entrega 1)
Cuando `msvc-config` no está disponible durante el arranque, cada cliente deberá (SHALL) poder iniciar usando los valores predeterminados suficientes incluidos en su propio `application.yml` local.

#### Scenario: Config Server no disponible
- **WHEN** inicia un cliente con `spring.config.import=optional:configserver:...` y no se puede contactar a `msvc-config`
- **THEN** el arranque continúa usando los valores locales para puerto y configuración necesaria del componente, sin que la ausencia del Config Server por sí sola impida iniciar la aplicación

### Requirement: Uso de configuración empaquetada (Entrega 1)
`msvc-config` deberá (SHALL) servir la configuración predeterminada desde los YAML empaquetados en el classpath con el perfil `native`.

#### Scenario: Configuración nativa
- **WHEN** inicia `msvc-config` con su configuración estándar
- **THEN** busca las configuraciones en `classpath:/configuration` y no requiere un repositorio o directorio externo
