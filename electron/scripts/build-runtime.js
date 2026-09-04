// Genera electron/runtime: un JRE recortado con jlink para empaquetar junto al instalador,
// evitando depender de un JDK instalado en la máquina del usuario final.
//
// El set de módulos no sale solo de `jdeps` (analisis estatico): Spring Data usa
// `javax.lang.model.SourceVersion` (java.compiler) en tiempo de arranque via reflexion,
// algo que jdeps no detecta. Verificado arrancando el jar real contra este runtime.
const { execFileSync } = require('node:child_process')
const { rmSync, existsSync } = require('node:fs')
const path = require('node:path')

const MODULES = [
  'java.base',
  'java.desktop', // ImageIO / validación de imágenes subidas
  'java.sql',
  'java.naming',
  'java.management',
  'java.instrument',
  'java.security.jgss',
  'java.compiler', // Spring Data AOT repository processor
  'jdk.unsupported',
  'jdk.crypto.ec',
  'jdk.zipfs',
]

const javaHome = process.env.JAVA_HOME
if (!javaHome) {
  console.error('JAVA_HOME no está definido. Se necesita un JDK 21 para generar el runtime con jlink.')
  process.exit(1)
}

const outputDir = path.join(__dirname, '..', 'runtime')
if (existsSync(outputDir)) rmSync(outputDir, { recursive: true, force: true })

const jlink = process.platform === 'win32' ? path.join(javaHome, 'bin', 'jlink.exe') : path.join(javaHome, 'bin', 'jlink')

execFileSync(jlink, [
  '--module-path', path.join(javaHome, 'jmods'),
  '--add-modules', MODULES.join(','),
  '--output', outputDir,
  '--strip-debug',
  '--no-man-pages',
  '--no-header-files',
  '--compress', 'zip-6',
], { stdio: 'inherit' })

console.log(`Runtime generado en ${outputDir}`)
