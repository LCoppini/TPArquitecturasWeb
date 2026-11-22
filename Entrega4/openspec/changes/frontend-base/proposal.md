# Proposal: Base del frontend web

## Why

La entrega necesita una interfaz web y hoy el repositorio no tiene ninguna. El diseño ya está aprobado (design system y pantallas desktop), pero faltan la base técnica sobre la que construir cada pantalla y las primeras pantallas que habilitan al resto: ingresar, crear cuenta y ver el catálogo de eventos. Este change crea esa base con una estructura que mantenga los componentes pequeños, reutilizables y con responsabilidades separadas.

## What Changes

- Nuevo proyecto `Entrega4/frontend` (Vite + React + TypeScript estricto), fuera del reactor Maven.
- Design system llevado a código desde una única fuente (`design/tokens.json`): variables CSS por tema claro y oscuro, escala tipográfica y mapeo a Tailwind y a los nombres que usa shadcn/ui.
- Componentes atómicos (`components/ui`): botón, campo de formulario, input, contraseña, casilla, chip, insignia, alerta, skeleton, avatar, logo, ícono y paginación.
- Componentes de maquetación (`components/layout`): navbar con y sin sesión, footer, contenedor y layouts de página y de autenticación.
- Cliente HTTP único contra el gateway, con manejo uniforme de errores (`ErrorResponse`) y token JWT.
- Sesión: ingreso, registro, cierre de sesión, lectura de los claims del token y vencimiento.
- Pantallas: ingresar, crear cuenta (con validaciones) y home con catálogo (listado, filtro por categoría, paginación y estados de carga, vacío y error).
- Documentación: ADR-0004 con el stack y README del frontend.

## Capabilities

### New Capabilities

- `frontend-base`: estructura, sistema de diseño en código, cliente HTTP, enrutado y reglas de organización de componentes.
- `frontend-autenticacion`: ingreso, registro y sesión del cliente.
- `frontend-catalogo`: listado público de eventos con filtro y paginación.

### Modified Capabilities

- Ninguna.

## Impact

- **Código**: solo `Entrega4/frontend` (nuevo), `docs/decisiones/0004-stack-frontend.md` y `openspec/changes/frontend-base`.
- **APIs consumidas** (sin cambios): `POST /api/usuarios/login`, `POST /api/usuarios/registro` y `GET /api/eventos`, todas públicas en el gateway.
- **Dependencias**: nuevas, solo de Node, justificadas en ADR-0004. No cambia el `pom.xml` ni ningún servicio Java.
- **Infraestructura**: en desarrollo, el servidor de Vite reenvía `/api` al gateway; no se toca el gateway.

## Fuera del alcance

- No se modifica ningún servicio Java. Quedan como dependencias a coordinar con el grupo, cada una en su propio change:
  - CORS en `msvc-gateway` (solo necesario para servir el frontend desde otro origen).
  - Datos que `GET /api/eventos` no entrega hoy y el diseño muestra: nombre y ciudad de la sede (`GET /api/lugares/{id}` exige rol de administrador u organizador) y disponibilidad por evento.
- Las demás pantallas (detalle, selección, fila virtual, pago, confirmación, mis entradas, devolución, perfil, notificaciones y errores) se incorporan en changes posteriores sobre esta base.
- La búsqueda por texto, ciudad, fecha y artista del hero y de la barra de filtros: el backend ofrece los parámetros, pero el diseño de esos controles (listas desplegables y calendario) se hace en un change aparte.
