# TicketYa · Frontend

Aplicación web de TicketYa. Hoy incluye la **cartelera** de eventos (filtro por categoría y paginación) y el **ingreso y registro** de usuarios. Decisión de stack: [`docs/decisiones/0004-stack-frontend.md`](../docs/decisiones/0004-stack-frontend.md). Especificación: [`openspec/changes/frontend-base`](../openspec/changes/frontend-base).

Stack: Vite · React 19 · TypeScript estricto · Tailwind v4 · patrón shadcn/ui · React Router · TanStack Query · React Hook Form + Zod · Vitest.

## Cómo ejecutarlo

Requiere Node 22 o superior y el gateway levantado en `http://localhost:8080` (con los servicios de usuarios y eventos).

```bash
cd Entrega4/frontend
npm install
npm run dev        # http://localhost:5173
```

En desarrollo, Vite reenvía `/api` al gateway, así que no hace falta CORS. Para apuntar a otro gateway:

```bash
VITE_GATEWAY_URL=http://otro-host:8080 npm run dev
```

## Scripts

| Comando | Qué hace |
| --- | --- |
| `npm run dev` | Servidor de desarrollo |
| `npm run build` | Typecheck y build de producción en `dist/` |
| `npm run typecheck` | Solo TypeScript |
| `npm run lint` | ESLint (incluye el límite de 150 líneas por archivo) |
| `npm test` | Tests con Vitest |
| `npm run tokens` | Regenera los estilos desde `design/tokens.json` |
| `npm run check:size` | Verifica que ningún archivo de `src/` pase las 150 líneas |

## Diseño: una sola fuente de verdad

Los colores, tipografías, radios y sombras salen de `design/tokens.json` (el mismo del design system). No se editan a mano `src/styles/tokens.css` ni `typography.css`: se generan con `npm run tokens`. `src/styles/theme.css` conecta esas variables con Tailwind, usando los nombres de shadcn/ui más los propios (`brand`, `ink`, `bone`, `fg-*`, `line-*`…).

## Organización

```
src/
  app/                 providers, rutas, layout y navegación
  components/
    ui/                átomos (Button, Input, Badge…). No conocen el negocio
    layout/            Navbar, Footer, paneles y contenedores
    feedback/          estados de error y vacío
  features/<nombre>/   api · hooks · components · pages · schemas · mappers · types
  lib/                 cliente HTTP, formatos, utilidades
  pages/               páginas transversales (404)
```

Reglas del proyecto:

- Un componente por archivo y ninguno supera las 150 líneas.
- Las páginas solo componen; la lógica va en hooks y las llamadas HTTP solo en `api/`.
- Los átomos de `components/ui` nunca importan de `features/`.
- El backend se traduce a modelos de vista en `mappers/`; los componentes no ven los DTO.
- No se inventan datos: lo que el backend no entrega (imagen, sede, disponibilidad) no se muestra.

## Sesión

Al ingresar se guarda el JWT en `sessionStorage` y se descodifica solo para la interfaz (la firma la valida el gateway). Al vencer el token, o ante un `401`, la sesión se cierra.
