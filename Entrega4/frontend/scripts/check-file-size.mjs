// Falla si algún archivo de src/ supera el máximo de líneas acordado en el diseño del cambio.
import { readdirSync, readFileSync, statSync } from 'node:fs'
import { join, relative } from 'node:path'

const MAX_LINES = 150
const IGNORED = new Set(['tokens.css', 'typography.css'])
const root = new URL('../src', import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1')

const walk = (dir) =>
  readdirSync(dir).flatMap((name) => {
    const path = join(dir, name)
    return statSync(path).isDirectory() ? walk(path) : [path]
  })

const offenders = walk(root)
  .filter((file) => /\.(ts|tsx|css)$/.test(file) && !IGNORED.has(file.split(/[\\/]/).pop()))
  .map((file) => ({ file, lines: readFileSync(file, 'utf8').split('\n').length }))
  .filter(({ lines }) => lines > MAX_LINES)

if (offenders.length > 0) {
  offenders.forEach(({ file, lines }) => console.error(`${relative(root, file)}: ${lines} líneas (máximo ${MAX_LINES})`))
  process.exit(1)
}
console.log(`Todos los archivos de src/ tienen ${MAX_LINES} líneas o menos.`)
