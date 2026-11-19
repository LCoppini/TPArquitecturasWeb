# Requisitos: identidad en los servicios de dominio

## ADDED Requirements

### Requirement: Los servicios reciben solo X-User-Id [Entrega 2]

Los microservicios de dominio DEBEN obtener la identidad del usuario únicamente
del header `X-User-Id` inyectado por el gateway.

#### Scenario: endpoint con identidad

- Dado que el gateway reenvió una solicitud autenticada
- Cuando un controller de dominio necesita saber quién opera
- Entonces lee `@RequestHeader("X-User-Id")`
- Y no declara ni lee ningún otro header de identidad

#### Scenario: ausencia del header

- Dado que una solicitud interna no trae `X-User-Id`
- Cuando el servicio la procesa
- Entonces la petición falla con un error de validación de entrada
- Y el servicio no inventa ni infiere un usuario

### Requirement: Sin X-User-Role en servicios ni Feign [Entrega 2]

Los servicios de dominio NO DEBEN declarar, propagar ni evaluar `X-User-Role`,
y las llamadas Feign NO DEBEN enviarlo.

#### Scenario: controllers de entrada y pagos

- Dado que se revisan `EntradaController`, `PagoController` y
  `RegistroPagoController`
- Cuando se buscan anotaciones `@RequestHeader`
- Entonces aparece `X-User-Id` y no aparece `X-User-Role`
- Y no existen los helpers `exigirRol` ni `exigirRolCliente`

#### Scenario: cliente Feign entre servicios

- Dado que `msvc-evento` invoca `POST /api/entradas/eventos/{idEvento}/cancelacion`
- Cuando construye la llamada con `EntradaClient`
- Entonces envía el `X-User-Id` del solicitante
- Y no envía ningún header de rol

### Requirement: Sin validacion de roles por Feign en dominio [Entrega 2]

Los servicios de dominio NO DEBEN consultar los roles del usuario a
`msvc-usuarios` para decidir si autorizan una operación.

#### Scenario: operaciones de evento

- Dado que el gateway ya autorizó la ruta por rol
- Cuando `EventoServiceImpl` ejecuta crear, actualizar, publicar, suspender o
  cancelar
- Entonces no invoca `validarRol` ni `tieneRol` contra `UsuarioClient`
- Y conserva únicamente la comparación entre el dueño del recurso y el
  `X-User-Id` recibido

#### Scenario: pertenencia del recurso

- Dado que un organizador intenta modificar un evento que no le pertenece
- Cuando el servicio valida la pertenencia
- Entonces responde acceso denegado comparando `idOrganizador` contra
  `X-User-Id`
- Y no depende del rol enviado por el cliente

### Requirement: Limitacion de llamadas interservicio documentada [Entrega 2]

La documentación DEBE registrar que las llamadas entre servicios (Feign) no
llevan JWT de usuario y quedan fuera del control de autenticación del gateway.

#### Scenario: documentacion de la limitacion

- Dado que se revisa el ADR de seguridad
- Cuando se busca el apartado de consecuencias
- Entonces consta que Feign no propaga el JWT
- Y consta el supuesto de que los servicios solo son accesibles por el gateway
