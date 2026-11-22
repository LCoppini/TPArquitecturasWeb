# frontend-autenticacion

## ADDED Requirements

### Requirement: Ingreso con correo y contraseña

La aplicación SHALL permitir ingresar con correo y contraseña contra `POST /api/usuarios/login`, validando el formato antes de enviar y mostrando el error correspondiente si las credenciales no son válidas.

#### Scenario: Ingreso correcto

- **WHEN** la persona envía credenciales válidas
- **THEN** se guarda la sesión y se la lleva al inicio con la navegación de sesión iniciada

#### Scenario: Credenciales incorrectas

- **WHEN** el backend responde `401`
- **THEN** se muestra la alerta "Correo o contraseña incorrectos" y los campos conservan lo escrito

#### Scenario: Campos inválidos

- **WHEN** el correo está vacío o no tiene formato válido, o la contraseña está vacía
- **THEN** no se envía la solicitud y cada campo muestra su mensaje de error

### Requirement: Registro de cliente

La aplicación SHALL permitir crear una cuenta con DNI, nombre, apellido, ciudad, correo y contraseña contra `POST /api/usuarios/registro`, con las mismas reglas que el backend.

#### Scenario: Registro correcto

- **WHEN** la persona completa el formulario con datos válidos
- **THEN** se crea la cuenta, se ingresa con esas credenciales y se la lleva al inicio

#### Scenario: Contraseña corta

- **WHEN** la contraseña tiene menos de 8 caracteres
- **THEN** no se envía la solicitud y el campo indica el mínimo de 8 caracteres

#### Scenario: Correo o DNI ya registrados

- **WHEN** el backend responde `409`
- **THEN** se muestra una alerta que indica que el correo o el DNI ya están registrados

### Requirement: Sesión del cliente

La aplicación SHALL conservar la sesión en `sessionStorage`, exponer los datos del token (`idUsuario` y `roles`) a la interfaz, cerrar la sesión al vencer el token y permitir cerrar sesión manualmente.

#### Scenario: Recarga de la página con sesión vigente

- **WHEN** se recarga la página y el token no venció
- **THEN** la sesión continúa y la navegación muestra el estado de sesión iniciada

#### Scenario: Token vencido

- **WHEN** el token vence o el backend responde `401` a una llamada autenticada
- **THEN** se limpia la sesión y la navegación vuelve al estado sin sesión

#### Scenario: Cierre de sesión

- **WHEN** la persona elige "Cerrar sesión" en el menú de cuenta
- **THEN** se borra el token y se la lleva al inicio sin sesión
