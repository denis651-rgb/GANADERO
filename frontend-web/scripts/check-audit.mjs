import { readFileSync } from 'node:fs'

const report = JSON.parse(readFileSync(process.argv[2] === '-' ? 0 : process.argv[2], 'utf8'))
const exceptions = new Set(['GHSA-qwww-vcr4-c8h2'])
const blocking = []

for (const vulnerability of Object.values(report.vulnerabilities ?? {})) {
  for (const advisory of vulnerability.via ?? []) {
    if (typeof advisory !== 'object') continue
    const id = String(advisory.url ?? '').split('/').pop()
    if (['high', 'critical'].includes(advisory.severity) && !exceptions.has(id)) {
      blocking.push(`${id}: ${advisory.title}`)
    }
  }
}

if (blocking.length) {
  console.error(`Vulnerabilidades bloqueantes:\n${blocking.join('\n')}`)
  process.exit(1)
}
console.log('Audit sin vulnerabilidades altas o críticas no exceptuadas.')
