import { describe, expect, it } from 'vitest'
import {
  errorFechaFin, errorHallazgos, erroresDosis, erroresViaLugar, fechaHoraLocal,
} from '@/features/sanidad/validacionesActividad'

describe('fecha de fin del plan', () => {
  it('rechaza una fecha de fin anterior a la de inicio', () => {
    expect(errorFechaFin('2026-06-01', '2026-05-31')).toBe('La fecha de fin no puede ser anterior a la fecha de inicio.')
  })

  it('acepta fin igual o posterior, o sin fin / sin inicio todavía', () => {
    expect(errorFechaFin('2026-06-01', '2026-06-01')).toBeUndefined()
    expect(errorFechaFin('2026-06-01', '2026-12-31')).toBeUndefined()
    expect(errorFechaFin('2026-06-01', '')).toBeUndefined()
    expect(errorFechaFin('', '2026-05-31')).toBeUndefined()
  })
})

describe('vía de administración y lugar anatómico', () => {
  it('no valida la compatibilidad mientras no haya vía, pero «otro» lugar siempre exige detalle', () => {
    expect(erroresViaLugar('', '', '', '')).toEqual({})
    expect(erroresViaLugar('', 'CUELLO', '', '')).toEqual({})
    expect(erroresViaLugar('', 'OTRO', '', '').detalleLugar).toBe('Indica el detalle del lugar.')
    expect(erroresViaLugar('', 'OTRO', '', 'Pezuña')).toEqual({})
  })

  it('exige lugar en las vías inyectables', () => {
    for (const via of ['SUBCUTANEA', 'INTRAMUSCULAR', 'INTRAVENOSA'] as const) {
      expect(erroresViaLugar(via, '', '', '').lugar).toBe('Una vía inyectable requiere indicar el lugar anatómico.')
      expect(erroresViaLugar(via, 'NO_APLICA', '', '').lugar).toBeDefined()
      expect(erroresViaLugar(via, 'TABLA_DEL_CUELLO', '', '')).toEqual({})
    }
  })

  it('la vía oral solo admite boca (o sin lugar)', () => {
    expect(erroresViaLugar('ORAL', 'BOCA', '', '')).toEqual({})
    expect(erroresViaLugar('ORAL', '', '', '')).toEqual({})
    expect(erroresViaLugar('ORAL', 'NO_APLICA', '', '')).toEqual({})
    expect(erroresViaLugar('ORAL', 'CUELLO', '', '').lugar).toBeDefined()
  })

  it('pour-on solo admite línea dorsal o lomo', () => {
    expect(erroresViaLugar('POUR_ON', 'LINEA_DORSAL', '', '')).toEqual({})
    expect(erroresViaLugar('POUR_ON', 'LOMO', '', '')).toEqual({})
    expect(erroresViaLugar('POUR_ON', 'BOCA', '', '').lugar).toBeDefined()
  })

  it('«otra» vía y «otro» lugar exigen su detalle', () => {
    expect(erroresViaLugar('OTRA', '', '  ', '').detalleVia).toBe('Indica el detalle de la vía.')
    expect(erroresViaLugar('OTRA', '', 'Intramamaria', '')).toEqual({})
    expect(erroresViaLugar('TOPICA', 'OTRO', '', '').detalleLugar).toBe('Indica el detalle del lugar.')
    expect(erroresViaLugar('TOPICA', 'OTRO', '', 'Pezuña')).toEqual({})
  })
})

describe('dosis', () => {
  it('exige cantidad en la dosis por peso', () => {
    expect(erroresDosis('POR_PESO', '', '', '').cantidad).toBe('La dosis por peso requiere una cantidad.')
    expect(erroresDosis('POR_PESO', '1', '', '')).toEqual({})
  })

  it('no exige cantidad en las demás dosis, pero si se escribe debe ser mayor que cero', () => {
    expect(erroresDosis('FIJA_POR_ANIMAL', '', '', '')).toEqual({})
    expect(erroresDosis('FIJA_POR_ANIMAL', '0', '', '').cantidad).toBe('La cantidad debe ser mayor que cero.')
    expect(erroresDosis('NO_APLICA', '0', '', '')).toEqual({})
  })

  it('mínima y máxima deben ser positivas y la máxima no puede ser menor que la mínima', () => {
    expect(erroresDosis('POR_PESO', '1', '0', '').minima).toBeDefined()
    expect(erroresDosis('POR_PESO', '1', '', '0').maxima).toBeDefined()
    expect(erroresDosis('POR_PESO', '1', '5', '2').maxima).toBe('La dosis máxima no puede ser menor que la mínima.')
    expect(erroresDosis('POR_PESO', '1', '2', '5')).toEqual({})
    expect(erroresDosis('POR_PESO', '1', '2', '2')).toEqual({})
  })
})

describe('hallazgos de la modalidad por hallazgo', () => {
  it('exige al menos uno solo en esa modalidad', () => {
    expect(errorHallazgos('POR_HALLAZGO', [])).toBe('Elige al menos un hallazgo que active la actividad.')
    expect(errorHallazgos('POR_HALLAZGO', ['CASO_CLINICO_ABIERTO'])).toBeUndefined()
    expect(errorHallazgos('PERIODICA', [])).toBeUndefined()
  })
})

describe('fecha y hora para el campo datetime-local', () => {
  it('devuelve la misma hora local con la que se guardó', () => {
    const local = new Date(2026, 4, 17, 9, 30)
    expect(fechaHoraLocal(local.toISOString())).toBe('2026-05-17T09:30')
  })

  it('devuelve vacío ante un valor no válido', () => {
    expect(fechaHoraLocal('no es una fecha')).toBe('')
  })
})
