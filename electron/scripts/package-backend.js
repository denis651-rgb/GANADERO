// Empaqueta el backend Spring Boot (mvnw) en modo offline sin tests.
//
// "./mvnw" no se puede lanzar como comando de npm en Windows: el shell que usan los scripts
// de npm ahí es cmd.exe, que no reconoce el shebang del wrapper de Unix y sólo sabe ejecutar
// mvnw.cmd. Este script elige el wrapper correcto según el SO en vez de depender del shell.
const { execFileSync } = require('node:child_process')
const path = require('node:path')

const backendDir = path.join(__dirname, '..', '..', 'backend')
const mvnw = process.platform === 'win32' ? path.join(backendDir, 'mvnw.cmd') : path.join(backendDir, 'mvnw')

execFileSync(mvnw, ['-q', '-o', 'clean', 'package', '-DskipTests'], {
  cwd: backendDir,
  stdio: 'inherit',
  // mvnw.cmd no es un ejecutable real: Node solo lo lanza a través de cmd.exe si se pide
  // explícitamente (desde la mitigación de Node para CVE-2024-27980 en spawn de .bat/.cmd).
  shell: process.platform === 'win32',
})
