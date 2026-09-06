import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { AnimalSummary } from '@/features/animales/types'
import { AnimalPicker } from './AnimalPicker'

const animales = [
  { id: 'animal-1', codigo: 'ANI-000001', nombre: 'Luna' },
  { id: 'animal-2', codigo: 'ANI-000002', nombre: 'Sol' },
] as AnimalSummary[]

describe('AnimalPicker', () => {
  it('filtra la lista de abajo por código o nombre', () => {
    render(<AnimalPicker animales={animales} cargando={false} value="" onChange={vi.fn()} />)

    expect(screen.getByText('ANI-000001', { exact: false })).toBeInTheDocument()
    expect(screen.getByText('ANI-000002', { exact: false })).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Buscar animal por código o nombre'), { target: { value: 'Sol' } })

    expect(screen.queryByText('ANI-000001', { exact: false })).not.toBeInTheDocument()
    expect(screen.getByText('ANI-000002', { exact: false })).toBeInTheDocument()
  })

  it('selecciona un animal de la lista con un click', () => {
    const onChange = vi.fn()
    render(<AnimalPicker animales={animales} cargando={false} value="" onChange={onChange} />)

    fireEvent.click(screen.getByRole('radio', { name: /ANI-000002/ }))

    expect(onChange).toHaveBeenCalledWith('animal-2')
  })

  it('marca como elegido el animal que coincide con value', () => {
    render(<AnimalPicker animales={animales} cargando={false} value="animal-1" onChange={vi.fn()} />)

    expect(screen.getByRole('radio', { name: /ANI-000001/ })).toBeChecked()
    expect(screen.getByRole('radio', { name: /ANI-000002/ })).not.toBeChecked()
  })
})
