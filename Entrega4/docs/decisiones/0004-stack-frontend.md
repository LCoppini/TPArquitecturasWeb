# ADR-0004: Stack del frontend web

- Estado: propuesta
- Fecha: 2026-11-22

## Contexto

La entrega incluye una interfaz web para clientes (catálogo, compra, entradas, devoluciones, perfil y notificaciones). El diseño ya está definido como design system propio (tokens, tipografías y componentes) y como pantallas desktop de 1440 px. El frontend vive dentro de `Entrega4`, consume solo el `msvc-gateway` (`AGENTS.md`: los clientes externos entran únicamente por el gateway) y el equipo necesita que el código sea fácil de mantener: componentes pequeños, con una sola responsabilidad y reutilizables.

`AGENTS.md` exige un ADR para toda dependencia o tecnología nueva.

## Decisión

| Necesidad | Elección |
|---|---|
| Build y dev server | Vite |
| UI | React 19 + TypeScript en modo estricto |
| Estilos | Tailwind CSS v4 con los tokens del design system como variables CSS |
| Componentes base | Patrón shadcn/ui: `class-variance-authority` + `tailwind-merge` + `@radix-ui/react-slot`; los componentes viven en el repo y se adaptan al diseño |
| Rutas | React Router |
| Datos del servidor | TanStack Query |
| Formularios y validación | React Hook Form + Zod |
| Tipografías | `@fontsource-variable` (Fraunces y DM Sans, autoalojadas) |
| Tests | Vitest + Testing Library |

Reglas que se derivan de la decisión:

- **Una sola fuente de verdad para el diseño**: `frontend/design/tokens.json`. Un script genera las variables CSS y las clases tipográficas. No se escriben colores, radios ni tamaños de letra a mano.
- **Estructura por funcionalidades** (`features/<nombre>`) con componentes atómicos en `components/ui` y de maquetación en `components/layout`.
- **Un componente por archivo**, páginas que solo componen, lógica en hooks y llamadas HTTP solo en `api/`.
- **Sin librerías de componentes cerradas** (MUI, Ant, etc.) ni de estado global adicional: el estado del servidor está en TanStack Query y la sesión en un contexto.

## Alternativas consideradas

- **Angular o Vue**: el equipo eligió React y shadcn/ui está pensado para React.
- **Next.js**: no hay necesidad de renderizado en servidor; sumaría un runtime Node y complejidad de despliegue sin beneficio para este alcance.
- **Reutilizar las clases `ty-*` del design system**: duplicaría la fuente de verdad del estilo y no se integra con Tailwind ni con shadcn.
- **Material UI o Chakra**: imponen su propio sistema visual y habría que sobrescribirlo contra el diseño.
- **Redux u otro estado global**: innecesario; los datos remotos los cubre TanStack Query.

## Consecuencias

- El frontend es un proyecto Node independiente dentro de `Entrega4/frontend`. **No es un módulo Maven**: no se agrega al `pom.xml` raíz ni afecta `mvn clean package`.
- Se necesita Node 20 o superior para desarrollar y compilar.
- En desarrollo, Vite reenvía `/api` al gateway (`localhost:8080`), de modo que no hace falta CORS. Para servir el frontend desde otro origen en producción, el gateway tendrá que configurar CORS: es un cambio de `msvc-gateway` que se tramita aparte.
- El diseño nuevo se incorpora cambiando `design/tokens.json` y regenerando los estilos.
- Hay una curva de aprendizaje de Tailwind para quien no lo conozca.
