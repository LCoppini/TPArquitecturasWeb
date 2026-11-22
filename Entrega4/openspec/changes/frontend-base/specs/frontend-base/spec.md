# frontend-base

## ADDED Requirements

### Requirement: Design system como única fuente de estilo

La aplicación SHALL generar sus variables CSS y su escala tipográfica a partir de `design/tokens.json`, con tema claro y tema oscuro, y SHALL NOT definir colores, radios ni tamaños de letra fuera de esos tokens.

#### Scenario: Se regeneran los estilos

- **WHEN** se ejecuta `npm run tokens`
- **THEN** se actualizan `tokens.css` y `typography.css` con los valores de `tokens.json` para ambos temas

#### Scenario: Un contenedor usa el tema oscuro

- **WHEN** un elemento tiene `data-theme="dark"`
- **THEN** sus descendientes usan los valores del tema oscuro sin cambiar de clases

### Requirement: Componentes pequeños y con una responsabilidad

Cada archivo de componente SHALL exportar un solo componente, las páginas SHALL limitarse a componer componentes, los átomos SHALL NOT importar código de `features/` y ningún archivo de código fuente SHALL superar las 150 líneas.

#### Scenario: Se verifica el tamaño de los archivos

- **WHEN** se ejecuta la verificación de tamaño sobre `src/`
- **THEN** ningún archivo `.ts` ni `.tsx` supera las 150 líneas

### Requirement: Cliente HTTP único

Toda llamada al backend SHALL pasar por el cliente HTTP común, que usa la base `/api`, envía el token cuando hay sesión y convierte cualquier respuesta de error en un `ApiError` con estado, mensaje y detalles por campo.

#### Scenario: El backend responde con error de validación

- **WHEN** el backend responde `400` con `details` por campo
- **THEN** se lanza un `ApiError` que conserva el estado, el mensaje y los detalles

#### Scenario: El backend no responde

- **WHEN** falla la conexión con el gateway
- **THEN** se lanza un `ApiError` de red con un mensaje que la interfaz puede mostrar

### Requirement: Rutas de la aplicación

La aplicación SHALL definir sus rutas en un único lugar y SHALL mostrar una página de "no encontrado" para cualquier ruta desconocida.

#### Scenario: Ruta inexistente

- **WHEN** la persona abre una ruta que no existe
- **THEN** ve la pantalla de página no encontrada con un enlace al inicio
