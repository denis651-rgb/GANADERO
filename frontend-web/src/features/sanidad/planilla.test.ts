import { describe, expect, it } from 'vitest'
import { datosPlanilla, filasCsvPlanilla, nombreArchivoPlanilla } from './planilla'
import type { AnimalElegibilidad, PlanSanitarioItem } from '@/features/sanidad/api'

function item(overrides: Partial<PlanSanitarioItem> = {}): PlanSanitarioItem {
  return {
    id: 'item-1', empresaId: 'e-1', planId: 'plan-1', identidadLogicaId: 'log-1', numeroVersion: 1,
    vigenteDesde: '2026-01-01', nombre: 'Vacunación Fiebre Aftosa', tipoActividad: 'VACUNACION',
    modalidad: 'MANUAL', modalidadConfig: {}, edadUnidad: 'DIAS', dosisTipoCalculo: 'FIJA_POR_ANIMAL',
    diasAlerta: 3, activo: true, version: 0,
    ...overrides,
  } as PlanSanitarioItem
}

function animal(overrides: Partial<AnimalElegibilidad> = {}): AnimalElegibilidad {
  return { id: 'a-1', codigo: 'BOV-001', sexo: 'MACHO', estado: 'ACTIVO', edadEstimada: false, elegible: true, motivos: [], ...overrides }
}

describe('datosPlanilla', () => {
  it('arma dosis fija por animal como cantidad + unidad', () => {
    const input = datosPlanilla({
      actividad: item({ dosisCantidad: 2, dosisUnidad: 'ML' }), fechaAplicacion: '2026-09-15', animales: [],
    })
    expect(input.dosisTexto).toBe('2 ml')
  })

  it('para dosis por peso, usa la etiqueta general en vez de un número', () => {
    const input = datosPlanilla({ actividad: item({ dosisTipoCalculo: 'POR_PESO' }), fechaAplicacion: '2026-09-15', animales: [] })
    expect(input.dosisTexto).toBe('Por peso')
  })

  it('sin medicamento (NO_APLICA), no muestra dosis', () => {
    const input = datosPlanilla({ actividad: item({ dosisTipoCalculo: 'NO_APLICA' }), fechaAplicacion: '2026-09-15', animales: [] })
    expect(input.dosisTexto).toBeUndefined()
  })

  it('prioriza la vía codificada sobre el texto libre', () => {
    const input = datosPlanilla({
      actividad: item({ viaAdministracionCodigo: 'SUBCUTANEA', viaAdministracion: 'texto libre viejo' }),
      fechaAplicacion: '2026-09-15', animales: [],
    })
    expect(input.viaAdministracion).toBe('Subcutánea')
  })

  it('arma el texto de edad marcando las estimadas', () => {
    const input = datosPlanilla({
      actividad: item(), fechaAplicacion: '2026-09-15',
      animales: [animal({ edadDias: 540, edadEstimada: true }), animal({ id: 'a-2', edadDias: null })],
    })
    expect(input.animales[0].edadTexto).toBe('≈ 540 días (estimada)')
    expect(input.animales[1].edadTexto).toBeUndefined()
  })
})

describe('filasCsvPlanilla', () => {
  it('solo incluye las filas de metadatos que tienen valor', () => {
    const input = datosPlanilla({ actividad: item({ dosisTipoCalculo: 'NO_APLICA' }), fechaAplicacion: '2026-09-15', propiedad: 'Estancia El Roble', animales: [] })
    const filas = filasCsvPlanilla(input)
    const etiquetas = filas.map((fila) => fila[0])
    expect(etiquetas).toContain('Actividad')
    expect(etiquetas).toContain('Propiedad')
    expect(etiquetas).not.toContain('Potrero')
    expect(etiquetas).not.toContain('Dosis')
  })

  it('agrega una fila por animal después del encabezado de la tabla', () => {
    const input = datosPlanilla({ actividad: item(), fechaAplicacion: '2026-09-15', animales: [animal({ codigo: 'BOV-009', sexo: 'HEMBRA' })] })
    const filas = filasCsvPlanilla(input)
    const indiceEncabezado = filas.findIndex((fila) => fila[0] === 'N°')
    expect(filas[indiceEncabezado + 1]).toEqual([1, 'BOV-009', '', 'Hembra', '', '', '', ''])
  })
})

describe('nombreArchivoPlanilla', () => {
  it('arma un nombre de archivo legible con la actividad y la fecha', () => {
    const input = datosPlanilla({ actividad: item({ nombre: 'Vacunación Fiebre Aftosa' }), fechaAplicacion: '2026-09-15', animales: [] })
    expect(nombreArchivoPlanilla(input, 'xlsx')).toBe('planilla-vacunacion-fiebre-aftosa-2026-09-15.xlsx')
  })
})
