import { describe, expect, it } from 'vitest'
import { construirCsv } from './csv'

describe('construirCsv', () => {
  it('arma encabezados y filas separados por coma', () => {
    expect(construirCsv(['Código', 'Peso'], [['BOV-001', 280]])).toBe('Código,Peso\nBOV-001,280')
  })

  it('entrecomilla valores con comas, comillas o saltos de línea', () => {
    expect(construirCsv(['Nota'], [['Hola, "mundo"']])).toBe('Nota\n"Hola, ""mundo"""')
  })

  it('convierte undefined/null en celdas vacías', () => {
    expect(construirCsv(['A', 'B'], [[undefined, 'x']])).toBe('A,B\n,x')
  })

  it('con encabezados vacíos no antepone una fila en blanco', () => {
    expect(construirCsv([], [['Actividad', 'Vacunación'], ['Fecha', '2026-09-15']])).toBe('Actividad,Vacunación\nFecha,2026-09-15')
  })
})
