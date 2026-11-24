# Tasks

## 1. Proyecto y diseño en código

- [x] 1.1 Crear `frontend/` con Vite, React, TypeScript estricto, Tailwind v4, alias `@` y proxy de `/api` al gateway; verificar que `npm run build` compila sin errores.
- [x] 1.2 Agregar `design/tokens.json` y `scripts/generate-tokens.mjs`; verificar que `npm run tokens` genera `tokens.css` (tema claro y oscuro) y `typography.css`.
- [ ] 1.3 Crear `styles/theme.css` con el mapeo a Tailwind y a los nombres de shadcn/ui, y las fuentes autoalojadas; verificar en el navegador que los títulos usan la serif y el texto la sans.

## 2. Componentes base

- [ ] 2.1 Crear los átomos de `components/ui` (ícono, botón, campo, input, contraseña, casilla, chip, insignia, alerta, skeleton, avatar, logo y paginación); verificar con tests de renderizado de los estados principales de cada uno.
- [ ] 2.2 Crear `components/layout` (navbar con y sin sesión, menú de cuenta, footer, contenedor, layout de página y de autenticación); verificar el navbar con y sin sesión.
- [x] 2.3 Crear `components/feedback` (estado de error y estado vacío); verificar con tests de renderizado y de las acciones.

## 3. Infraestructura

- [x] 3.1 Crear `lib/http` (cliente, `ApiError`) y `lib/format` (precio y fecha); verificar con tests: error con detalles, error de red, `$ 48.500` y `Sáb 14 nov · 21:00`.
- [x] 3.2 Crear los providers, el router y las rutas con la página de no encontrado; verificar que una ruta desconocida muestra la pantalla 404.

## 4. Autenticación

- [x] 4.1 Crear `features/auth` (api, token, sesión con vencimiento, `AuthProvider` y `useAuth`); verificar con tests la lectura de claims, el vencimiento y el cierre de sesión.
- [x] 4.2 Crear los esquemas Zod, `LoginForm` y la página de ingreso; verificar con tests los escenarios de validación, credenciales incorrectas e ingreso correcto.
- [x] 4.3 Crear `RegisterForm` y la página de registro; verificar con tests los escenarios de contraseña corta, `409` y registro correcto.

## 5. Catálogo

- [x] 5.1 Crear `features/events` (api, tipos, mapper, hook de consulta); verificar con tests del mapper: precio mínimo, estados, y datos ausentes.
- [ ] 5.2 Crear los componentes de la tarjeta (póster, fecha, estado, datos y precio), la grilla, los skeletons y el filtro de categorías; verificar con tests de renderizado de los estados disponible y agotado (el estado "últimas entradas" no se muestra: el backend no entrega disponibilidad).
- [ ] 5.3 Crear la página de inicio con hero, filtro, grilla y paginación sincronizados con la dirección; verificar los escenarios de filtro, página, carga, vacío y error.

## 6. Documentación y cierre

- [x] 6.1 Escribir `frontend/README.md` (cómo instalar, ejecutar, probar y regenerar los tokens; reglas de organización) y actualizar el README raíz.
- [x] 6.2 Ejecutar `npm run lint`, `npm run typecheck`, `npm test` y `npm run build` y verificar que pasan.
- [ ] 6.3 Con el gateway y los servicios levantados, ingresar, crear una cuenta y ver el catálogo contra el backend real.
- [ ] 6.4 Verificar el alcance: `git status` solo muestra cambios en `frontend/`, `docs/decisiones/0004-stack-frontend.md`, `openspec/changes/frontend-base` y el README raíz; no se tocó ningún servicio Java ni el `pom.xml`; ningún archivo de `src/` supera las 150 líneas.
