// Genera src/styles/tokens.css y src/styles/typography.css a partir de design/tokens.json.
// El JSON es la única fuente de verdad del diseño: no se editan los CSS generados a mano.
import { readFileSync, writeFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const tokens = JSON.parse(readFileSync(resolve(root, 'design/tokens.json'), 'utf8'))

const NON_COLOR_FAMILIES = ['spacing', 'radius', 'shadow', 'layout', 'duration', 'easing', 'zIndex']
const HEADER = '/* Generado por scripts/generate-tokens.mjs desde design/tokens.json. No editar a mano. */\n'

const resolveAlias = (value) => value.replace(/\{([^}]+)\}/g, (_, name) => `var(--ty-${name})`)

/** Devuelve [light, dark] para un token; los valores sin tema se repiten solo en :root. */
const splitThemes = (value) =>
  typeof value === 'string'
    ? { light: resolveAlias(value), dark: null }
    : { light: resolveAlias(value.light), dark: resolveAlias(value.dark) }

const light = []
const dark = []
const collect = (list) => {
  for (const { name, value } of list) {
    const themes = splitThemes(value)
    light.push(`  --ty-${name}: ${themes.light};`)
    if (themes.dark !== null) dark.push(`  --ty-${name}: ${themes.dark};`)
  }
}

collect(tokens.color.tokens)
NON_COLOR_FAMILIES.forEach((family) => collect(tokens[family].tokens))
Object.entries(tokens.type.families).forEach(([name, stack]) => light.push(`  --ty-font-${name}: ${stack};`))

const tokensCss = `${HEADER}:root {\n${light.join('\n')}\n}\n\n[data-theme='dark'] {\n${dark.join('\n')}\n}\n`

const styleToUtility = (style) => {
  const rules = [
    `font-family: var(--ty-font-${style.family});`,
    `font-size: ${style.fontSize};`,
    `line-height: ${style.lineHeight};`,
    `font-weight: ${style.fontWeight};`,
  ]
  if (style.letterSpacing) rules.push(`letter-spacing: ${style.letterSpacing};`)
  if (style.opticalSize) rules.push(`font-variation-settings: 'opsz' ${style.opticalSize};`)
  if (style.name === 'overline') rules.push('text-transform: uppercase;')
  return `@utility text-${style.name} {\n  ${rules.join('\n  ')}\n}\n`
}

const utilities = tokens.type.groups.flatMap((group) =>
  group.styles.map((style) => styleToUtility({ ...style, family: group.family })),
)

writeFileSync(resolve(root, 'src/styles/tokens.css'), tokensCss)
writeFileSync(resolve(root, 'src/styles/typography.css'), `${HEADER}${utilities.join('\n')}`)
console.log(`tokens.css (${light.length} claros, ${dark.length} oscuros) y typography.css (${utilities.length} estilos)`)
