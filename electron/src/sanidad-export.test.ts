import { describe, expect, it } from 'vitest'
import { construirPlanillaSanitaria, type PlanillaSanitariaInput } from './sanidad-export'

function input(overrides: Partial<PlanillaSanitariaInput> = {}): PlanillaSanitariaInput {
  return {
    actividad: 'Vacunación Fiebre Aftosa',
    fecha: '2026-09-15',
    propiedad: 'Estancia El Roble',
    potrero: 'Potrero 2',
    producto: 'Vacuna antiaftosa trivalente',
    dosisTexto: '2 ml',
    viaAdministracion: 'Subcutánea',
    animales: [
      { codigo: 'BOV-001', nombre: 'Lucero', sexo: 'MACHO', edadTexto: '18 meses' },
      { codigo: 'BOV-002', sexo: 'HEMBRA' },
    ],
    ...overrides,
  }
}

function celdasHoja(workbook: ReturnType<typeof construirPlanillaSanitaria>) {
  return workbook.getWorksheet('Planilla de campo')!
}

describe('construirPlanillaSanitaria', () => {
  it('arma el título con el nombre de la actividad', () => {
    const hoja = celdasHoja(construirPlanillaSanitaria(input()))
    expect(hoja.getCell('A1').value).toBe('Planilla de campo — Vacunación Fiebre Aftosa')
  })

  it('agrega una fila por animal con casilla en blanco para tickear', () => {
    const hoja = celdasHoja(construirPlanillaSanitaria(input()))
    const filaUltimoAnimal = hoja.getRow(hoja.rowCount - 2) // después de la fila vacía y la firma
    expect(filaUltimoAnimal.getCell(2).value).toBe('BOV-002')
    expect(filaUltimoAnimal.getCell(6).value).toBe('☐')
    expect(filaUltimoAnimal.getCell(7).value).toBe('')
  })

  it('omite del encabezado los datos que no vienen', () => {
    const hoja = celdasHoja(construirPlanillaSanitaria(input({ potrero: undefined, lote: undefined, instrucciones: undefined })))
    const texto = hoja.getSheetValues().flat().filter((valor) => typeof valor === 'string').join(' | ')
    expect(texto).not.toContain('Potrero:')
    expect(texto).not.toContain('Instrucciones:')
  })

  it('incluye instrucciones y días de retiro cuando vienen', () => {
    const hoja = celdasHoja(construirPlanillaSanitaria(input({
      instrucciones: 'Aplicar en tabla del cuello', retiroCarneDias: 14, retiroLecheDias: 3,
    })))
    const texto = hoja.getSheetValues().flat().filter((valor) => typeof valor === 'string').join(' | ')
    expect(texto).toContain('Instrucciones: Aplicar en tabla del cuello')
    expect(texto).toContain('Retiro de carne: 14 días')
    expect(texto).toContain('Retiro de leche: 3 días')
  })

  it('traduce el sexo a español', () => {
    const hoja = celdasHoja(construirPlanillaSanitaria(input()))
    const encabezado = COLUMNAS_ENCABEZADO(hoja)
    expect(encabezado).toEqual(['N°', 'Código', 'Nombre', 'Sexo', 'Edad', 'Aplicado', 'Dosis aplicada', 'Observaciones'])
  })
})

function COLUMNAS_ENCABEZADO(hoja: ReturnType<typeof celdasHoja>): unknown[] {
  const filas = hoja.getSheetValues()
  const filaEncabezado = filas.find((fila) => Array.isArray(fila) && fila.includes('Código'))
  return (filaEncabezado as unknown[]).filter((valor) => valor !== undefined && valor !== null)
}
