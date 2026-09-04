import { describe, expect, it } from 'vitest'
import { buildResumenAlertItems } from './alertasResumen'
import type { GanaderoAlert } from '@/features/alertas/api'

function alerta(overrides: Partial<GanaderoAlert> = {}): GanaderoAlert {
  return {
    id: 'a-1',
    tipo: 'VACUNA_VENCIDA',
    titulo: 'Vacuna vencida',
    mensaje: '3 animales con vacuna vencida',
    severidad: 'CRITICA',
    fechaProgramada: '2026-08-01T00:00:00Z',
    origenTipo: 'JORNADA',
    estado: 'PENDIENTE',
    metadata: {},
    ...overrides,
  }
}

describe('buildResumenAlertItems', () => {
  it('descarta alertas resueltas, canceladas o ya atendidas', () => {
    const fuentes = [
      { alertas: [alerta({ id: 'a-1', estado: 'RESUELTA' }), alerta({ id: 'a-2', estado: 'CANCELADA' }), alerta({ id: 'a-3', estado: 'ATENDIDA' }), alerta({ id: 'a-4', estado: 'PENDIENTE' })], severidad: 'danger' as const, seccion: 'casos' as const, seccionLabel: 'Casos clínicos' },
    ]
    const items = buildResumenAlertItems(fuentes)
    expect(items).toHaveLength(1)
    expect(items[0].id).toBe('a-4')
  })

  it('ordena danger antes que warning sin importar el orden de las fuentes', () => {
    const fuentes = [
      { alertas: [alerta({ id: 'w-1' })], severidad: 'warning' as const, seccion: 'tratamientos' as const, seccionLabel: 'Tratamientos' },
      { alertas: [alerta({ id: 'd-1' })], severidad: 'danger' as const, seccion: 'casos' as const, seccionLabel: 'Casos clínicos' },
    ]
    const items = buildResumenAlertItems(fuentes)
    expect(items.map((item) => item.id)).toEqual(['d-1', 'w-1'])
  })

  it('mapea título, mensaje y sección de destino', () => {
    const fuentes = [
      { alertas: [alerta({ id: 'v-1', titulo: 'Vacuna próxima', mensaje: '2 animales con vacuna próxima a vencer' })], severidad: 'warning' as const, seccion: 'jornadas' as const, seccionLabel: 'Jornadas' },
    ]
    const [item] = buildResumenAlertItems(fuentes)
    expect(item).toMatchObject({ mensaje: 'Vacuna próxima', detalle: '2 animales con vacuna próxima a vencer', seccion: 'jornadas', seccionLabel: 'Jornadas' })
  })
})
