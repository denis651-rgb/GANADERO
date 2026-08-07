import { describe, expect, it } from 'vitest'
import { buildDashboardModel, formatPesoKg, type DashboardModel } from './dashboardModel'
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
    const model = buildDashboardModel(resumenVacio, { operacionesPendientes: 0, conflictos: 0, archivosPendientes: 0 }, 'TODA_EMPRESA')
    expect(model.tieneDatos).toBe(false)
  })

  it('detecta datos por animales', () => {
    const model = buildDashboardModel(resumenConDatos, { operacionesPendientes: 0, conflictos: 0, archivosPendientes: 0 }, 'TODA_EMPRESA')
    expect(model.tieneDatos).toBe(true)
  })

  it('detecta datos por pendientes locales', () => {
    const model = buildDashboardModel(resumenVacio, { operacionesPendientes: 3, conflictos: 0, archivosPendientes: 0 }, 'TODA_EMPRESA')
    expect(model.tieneDatos).toBe(true)
  })

  it('detecta datos por conflictos locales', () => {
    const model = buildDashboardModel(resumenVacio, { operacionesPendientes: 0, conflictos: 1, archivosPendientes: 0 }, 'TODA_EMPRESA')
    expect(model.tieneDatos).toBe(true)
  })

  it('expone el alcance recibido', () => {
    const model = buildDashboardModel(resumenConDatos, { operacionesPendientes: 0, conflictos: 0, archivosPendientes: 0 }, 'PROPIEDADES_ASIGNADAS')
    expect(model.scope).toBe('PROPIEDADES_ASIGNADAS')
  })

  it('preserva resumen y datos locales', () => {
    const model: DashboardModel = buildDashboardModel(
      resumenConDatos,
      { operacionesPendientes: 2, conflictos: 0, archivosPendientes: 1, ultimaSincronizacion: '2026-08-07T12:00:00Z' },
      'TODA_EMPRESA',
    )
    expect(model.resumen.totalAnimales).toBe(25)
    expect(model.resumen.gananciaPromedioKg).toBe(0.62)
    expect(model.local.operacionesPendientes).toBe(2)
    expect(model.local.archivosPendientes).toBe(1)
    expect(model.local.ultimaSincronizacion).toBe('2026-08-07T12:00:00Z')
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
