import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import type { ValidacionMovimiento } from '@/features/movimientos/api'
import { MovimientoValidationDialog } from './MovimientoValidationDialog'

const MENSAJE_LARGO = 'El animal no tiene una prueba diagnóstica registrada desde su ingreso; no puede salir de cuarentena.'

function renderDialog(validation: ValidacionMovimiento) {
  render(
    <MemoryRouter>
      <MovimientoValidationDialog open validation={validation} onClose={vi.fn()} onConfirm={vi.fn()} />
    </MemoryRouter>,
  )
}

describe('MovimientoValidationDialog', () => {
  it('muestra el mensaje completo, sin truncarlo', () => {
    renderDialog({
      valid: false, total: 1, validos: 0, invalidos: 1,
      resultados: [{ animalId: 'a-1', estado: 'INVALIDO', error: 'MOVEMENT_CUARENTENA_SIN_PRUEBA_DIAGNOSTICA', mensaje: MENSAJE_LARGO }],
    })
    expect(screen.getByText(MENSAJE_LARGO)).toBeInTheDocument()
  })

  it('ofrece "Registrar prueba diagnóstica" solo para ese error puntual', () => {
    renderDialog({
      valid: false, total: 2, validos: 1, invalidos: 1,
      resultados: [
        { animalId: 'a-1', estado: 'INVALIDO', error: 'MOVEMENT_CUARENTENA_SIN_PRUEBA_DIAGNOSTICA', mensaje: MENSAJE_LARGO },
        { animalId: 'a-2', estado: 'VALIDO' },
      ],
    })
    const link = screen.getByRole('link', { name: 'Registrar prueba diagnóstica' })
    expect(link).toHaveAttribute('href', '/sanidad?seccion=jornadas&tipoJornada=PRUEBA_DIAGNOSTICA')
    expect(screen.getAllByRole('link', { name: 'Registrar prueba diagnóstica' })).toHaveLength(1)
  })

  it('no ofrece el atajo para otros errores de validación', () => {
    renderDialog({
      valid: false, total: 1, validos: 0, invalidos: 1,
      resultados: [{ animalId: 'a-1', estado: 'INVALIDO', error: 'ANIMAL_STATUS_NOT_ALLOWED', mensaje: 'El animal está MUERTO.' }],
    })
    expect(screen.queryByRole('link', { name: 'Registrar prueba diagnóstica' })).not.toBeInTheDocument()
  })
})
