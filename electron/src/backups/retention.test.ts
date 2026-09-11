import { describe, expect, it } from 'vitest'
import { calcularSobrantes, type RespaldoResumen } from './retention'

function respaldo(nombre: string, fecha: string, integridad: RespaldoResumen['integridad'] = 'VALIDA', estado = 'CREADO_LOCALMENTE'): RespaldoResumen {
  return { nombreArchivo: nombre, fechaCreacion: fecha, estado, integridad }
}

describe('calcularSobrantes', () => {
  it('conserva los últimos N diarios y marca el resto como sobrante', () => {
    const respaldos = [
      respaldo('d1', '2026-01-10T20:00:00Z'),
      respaldo('d2', '2026-01-09T20:00:00Z'),
      respaldo('d3', '2026-01-08T20:00:00Z'),
      respaldo('d4', '2026-01-07T20:00:00Z'),
    ]
    const sobrantes = calcularSobrantes(respaldos, { retencionDiarios: 2, retencionSemanales: 0, retencionMensuales: 0 })

    expect(sobrantes).toContain('d3')
    expect(sobrantes).toContain('d4')
    expect(sobrantes).not.toContain('d1')
    expect(sobrantes).not.toContain('d2')
  })

  it('nunca marca como sobrante el único respaldo válido, aunque la retención diaria sea 0', () => {
    const respaldos = [respaldo('unico', '2020-01-01T00:00:00Z')]
    const sobrantes = calcularSobrantes(respaldos, { retencionDiarios: 0, retencionSemanales: 0, retencionMensuales: 0 })
    expect(sobrantes).not.toContain('unico')
  })

  it('nunca marca como sobrante uno en progreso o de integridad desconocida', () => {
    const respaldos = [
      respaldo('valido-reciente', '2026-01-10T20:00:00Z'),
      respaldo('en-progreso', '2020-01-01T00:00:00Z', 'DESCONOCIDA', 'CREANDO'),
      respaldo('copiando', '2020-01-02T00:00:00Z', 'DESCONOCIDA', 'COPIANDO_A_CARPETA_EXTERNA'),
    ]
    const sobrantes = calcularSobrantes(respaldos, { retencionDiarios: 1, retencionSemanales: 0, retencionMensuales: 0 })
    expect(sobrantes).not.toContain('en-progreso')
    expect(sobrantes).not.toContain('copiando')
  })

  it('conserva uno por cada una de las últimas semanas y meses configurados', () => {
    const respaldos = [
      respaldo('semana-actual', '2026-01-12T00:00:00Z'), // lunes
      respaldo('semana-anterior', '2026-01-05T00:00:00Z'),
      respaldo('mes-anterior', '2025-12-01T00:00:00Z'),
    ]
    const sobrantes = calcularSobrantes(respaldos, { retencionDiarios: 0, retencionSemanales: 2, retencionMensuales: 2 })
    expect(sobrantes).toHaveLength(0)
  })
})
