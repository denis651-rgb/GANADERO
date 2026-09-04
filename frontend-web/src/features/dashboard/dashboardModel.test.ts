import { describe, expect, it } from 'vitest'
import { buildAttentionItems, buildDashboardModel, formatPesoKg } from './dashboardModel'
import type { DashboardResumen } from './api'

const resumenVacio: DashboardResumen = {
  totalAnimales: 0,
  animalesEnPotrero: 0,
  lotesActivos: 0,
  potrerosActivos: 0,
  pesoPromedioKg: undefined,
  gananciaPromedioKg: undefined,
  pesajesUltimos7Dias: 0,
  movimientosUltimos7Dias: 0,
  animalesSinPesaje: 0,
  animalesPorCategoria: [],
  animalesPorPotrero: [],
  animalesPorLote: [],
  pesajesRecientes: [],
  alertas: [],
  generadoEn: '',
}

const resumenConDatos: DashboardResumen = {
  ...resumenVacio,
  totalAnimales: 25,
  lotesActivos: 3,
  potrerosActivos: 4,
  pesoPromedioKg: 312.4,
  gananciaPromedioKg: 0.62,
  animalesPorCategoria: [{ nombre: 'Vacas', total: 18 }, { nombre: 'Terneros', total: 7 }],
  pesajesRecientes: [{ id: 'p1', animalId: 'a1', animalCodigo: 'A-001', animalNombre: 'Vaca 1', fecha: '2026-08-05', pesoKg: 410 }],
  alertas: [{ tipo: 'SIN_PESAJE', mensaje: 'Animales sin pesaje', severidad: 'warning', total: 4 }],
}

describe('buildDashboardModel', () => {
  it('marca sin datos cuando no hay registros', () => {
    const model = buildDashboardModel(resumenVacio)
    expect(model.tieneDatos).toBe(false)
  })

  it('detecta datos por animales', () => {
    const model = buildDashboardModel(resumenConDatos)
    expect(model.tieneDatos).toBe(true)
  })

  it('preserva el resumen', () => {
    const model = buildDashboardModel(resumenConDatos)
    expect(model.resumen.totalAnimales).toBe(25)
    expect(model.resumen.gananciaPromedioKg).toBe(0.62)
  })
})

describe('formatPesoKg', () => {
  it('formatea con unidad kg', () => {
    expect(formatPesoKg(312.4)).toBe('312,4 kg')
  })

  it('devuelve guion largo para valores nulos', () => {
    expect(formatPesoKg(undefined)).toBe('—')
    expect(formatPesoKg(null as unknown as undefined)).toBe('—')
  })
})

describe('buildAttentionItems', () => {
  it('no genera items cuando no hay alertas ni animales sin pesaje', () => {
    expect(buildAttentionItems(resumenVacio)).toEqual([])
  })

  it('agrega el recordatorio de pesaje como severidad warning', () => {
    const items = buildAttentionItems({ ...resumenVacio, animalesSinPesaje: 5 })
    expect(items).toEqual([
      {
        key: 'SIN_PESAJE_RECIENTE',
        severidad: 'warning',
        mensaje: '5 animales sin pesaje reciente',
        detalle: 'Registra controles para mantener actualizado el seguimiento productivo.',
        actionHref: '/pesajes',
        actionLabel: 'Registrar pesaje',
      },
    ])
  })

  it('ordena danger antes que warning antes que info, sin importar el orden de origen', () => {
    const resumen: DashboardResumen = {
      ...resumenVacio,
      animalesSinPesaje: 2,
      alertas: [
        { tipo: 'INFO_X', mensaje: 'Aviso informativo', severidad: 'info', total: 1 },
        { tipo: 'DANGER_X', mensaje: 'Vencimiento crítico', severidad: 'danger', total: 3 },
      ],
    }

    const items = buildAttentionItems(resumen)

    expect(items.map((item) => item.severidad)).toEqual(['danger', 'warning', 'info'])
    expect(items[0].mensaje).toBe('Vencimiento crítico')
  })
})
