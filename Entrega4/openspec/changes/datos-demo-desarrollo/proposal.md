# Proposal: datos de demostración para desarrollo

## Por qué
Para ver las pantallas del frontend (cartelera, ingreso, menú de cuenta) hay que cargar a mano usuarios, lugares y eventos en tres servicios. Eso demora la prueba y no es repetible entre integrantes del grupo.

## Qué cambia
Se agrega un seed opcional, activo solo con el perfil `dev-seed`, en tres servicios:

- **msvc-usuarios:** un usuario por rol con una contraseña conocida.
- **msvc-ubicacion:** cuatro lugares con sus sectores.
- **msvc-evento:** unos veinte eventos de distintas categorías y estados, con fechas relativas a hoy.

Sin el perfil no ocurre nada. Cada seed es idempotente: no duplica datos si ya existen.

## Fuera de alcance
- Datos de msvc-entrada (compras, entradas) y msvc-preferencias.
- Cambios en la lógica de negocio, contratos o endpoints de los servicios.
- Migraciones: el seed usa los repositorios existentes.

## Riesgos
- Los eventos referencian lugares, sectores y organizador por id, sin consultar los otros servicios. Solo funciona sobre bases vacías y con los seeds cargados en orden. Está documentado y cada seed lo registra en el log.
- Las credenciales de demostración son públicas. El perfil `dev-seed` no debe usarse fuera de desarrollo.
