# Design: Base del frontend web

## Context

El frontend consume solo el `msvc-gateway` (`:8080`). Los endpoints que usa esta primera parte son públicos: `POST /api/usuarios/login`, `POST /api/usuarios/registro` y `GET /api/eventos`. El token es un JWT RS256 emitido por `msvc-usuarios` con los claims `idUsuario` y `roles` y expiración de 15 minutos (ADR-0003). El diseño de referencia está en el design system "TicketYa" y en las pantallas desktop de 1440 px.

## Goals / Non-Goals

**Goals**

- Que cada archivo tenga una sola razón de cambio y quede corto.
- Que el diseño tenga una única fuente de verdad y que cambiarlo no obligue a tocar componentes.
- Que las pantallas siguientes se armen componiendo lo que ya existe.

**Non-Goals**

- Responsive móvil: el diseño aprobado es desktop.
- Internacionalización: el producto es solo en español rioplatense.
- Tocar el backend.

## Decisions

### 1. Ubicación y alcance del build

El proyecto vive en `Entrega4/frontend` y no es un módulo Maven. `mvn clean package` no lo compila ni lo ve. Se agrega al `.gitignore` solo `node_modules` y `dist` (dentro de `frontend/`).

### 2. Diseño como datos: de `tokens.json` a CSS

`design/tokens.json` es la copia del design system. El script `scripts/generate-tokens.mjs` produce dos archivos que se versionan:

- `src/styles/tokens.css`: variables con prefijo `--ty-` para el tema claro (`:root`) y el oscuro (`[data-theme="dark"]`).
- `src/styles/typography.css`: una utilidad por estilo tipográfico (`text-display`, `text-h1`… `text-overline`, `text-button`).

El prefijo evita choques con los espacios de nombres de Tailwind (`--shadow-*`, `--radius-*`). `src/styles/theme.css` mapea esas variables a Tailwind con `@theme inline`, con dos grupos de nombres:

- **Nombres de shadcn/ui** (`background`, `foreground`, `card`, `primary`, `muted`, `border`, `input`, `ring`…), para que cualquier componente agregado después con la CLI de shadcn tome el estilo de TicketYa sin cambios.
- **Nombres propios** (`page`, `surface`, `fg`, `brand`, `line`, estados `success/warning/error/info`), más expresivos para el código de la aplicación.

### 3. Organización del código

```
src/
  app/          providers, router y rutas
  components/
    ui/         átomos sin conocimiento del dominio
    layout/     navbar, footer, contenedores y layouts
    feedback/   estados de error y vacío
  features/
    auth/       api, hooks, schemas, components, pages
    events/     api, hooks, mappers, types, components, pages
  lib/          cliente HTTP, formato, utilidades
  styles/
```

Reglas: un componente por archivo; las páginas solo componen; la lógica y el estado van en hooks; las llamadas HTTP solo en `api/`; los átomos no importan nada de `features/`; ningún archivo de código supera las 150 líneas (se comprueba en la verificación).

### 4. Cliente HTTP

Una función `http` sobre `fetch` con base `/api`, JSON por defecto y token en `Authorization: Bearer`. Todo error se traduce a `ApiError` (status, mensaje y detalles por campo, a partir de `ErrorResponse`). Una respuesta 401 en una llamada autenticada limpia la sesión. En desarrollo, el proxy de Vite reenvía `/api` al gateway, por lo que no se necesita CORS.

### 5. Sesión

El token se guarda en `sessionStorage` (se pierde al cerrar la pestaña, acorde con una expiración de 15 minutos). La interfaz lee los claims del payload solo para mostrar datos y decidir qué mostrar; la autoridad sigue siendo el gateway, que valida la firma. Un temporizador cierra la sesión al vencer el token. `AuthProvider` expone `useAuth()`; no hay estado global adicional.

### 6. Catálogo y datos que la API no entrega

`EventoResponse` trae nombre, tipo, fecha, estado y precios por sector, pero no la sede, la imagen ni la disponibilidad. El mapper de eventos produce un modelo de vista con esos campos opcionales y la interfaz muestra solo lo que existe: no se inventan datos. El precio "Desde" es el menor precio entre los sectores. El arte del evento es el póster generativo del diseño, elegido por el id del evento. Las categorías del filtro se definen en un único archivo, con la etiqueta y el valor `tipo` que espera el backend; ese valor es un supuesto a confirmar con los datos reales cargados.

### 7. Registro según el contrato

El diseño del registro pide "nombre y apellido" en un campo, correo y contraseña. `RegistroRequest` exige además `dni` y `ciudad` y separa `nombre` y `apellido`. El formulario sigue el contrato del backend, con la misma estética, y las reglas de validación de Zod reflejan las de Bean Validation.

## Risks / Trade-offs

- **Valores de `tipo` desconocidos**: si los datos cargados usan otros nombres de categoría, el filtro no devolverá resultados. Mitigación: un único archivo de categorías y una verificación manual contra datos reales.
- **Sede ausente en las tarjetas**: el diseño muestra "Lugar · Ciudad". Hasta que el backend lo exponga, la tarjeta no muestra esa línea. Es una dependencia a resolver en `msvc-evento`.
- **Token en `sessionStorage`**: queda expuesto a XSS, como cualquier almacenamiento accesible por JavaScript. Se acepta por la vida corta del token y porque el backend no emite cookies.
- **Duplicación visual respecto del design system**: los componentes se reescriben con Tailwind. Se mitiga porque ambos leen los mismos tokens.

## Migration Plan

Sin migración: es código nuevo. Para volver atrás basta con borrar `Entrega4/frontend`, `docs/decisiones/0004-stack-frontend.md` y este change; ningún servicio depende de ellos.
