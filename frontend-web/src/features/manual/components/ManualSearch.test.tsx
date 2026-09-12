import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { describe, expect, it } from 'vitest'
import { ManualSearch } from './ManualSearch'

function renderSearch() {
  return render(
    <MemoryRouter initialEntries={['/manual']}>
      <Routes>
        <Route path="/manual" element={<ManualSearch />} />
        <Route path="/manual/:chapterId" element={<div>Página de destino</div>} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('ManualSearch', () => {
  it('con menos de 2 caracteres no muestra resultados', () => {
    renderSearch()
    fireEvent.change(screen.getByPlaceholderText(/edad aproximada/), { target: { value: 'e' } })
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
  })

  it('muestra resultados con capítulo, encabezado y fragmento resaltado', () => {
    renderSearch()
    fireEvent.change(screen.getByPlaceholderText(/edad aproximada/), { target: { value: 'edad aproximada' } })

    const options = screen.getAllByRole('button')
    expect(options.length).toBeGreaterThan(0)
    expect(screen.getAllByText('Compras').length).toBeGreaterThan(0)
    expect(screen.getAllByText('edad aproximada', { exact: false }).length).toBeGreaterThan(0)
  })

  it('sin resultados muestra el mensaje correspondiente', () => {
    renderSearch()
    fireEvent.change(screen.getByPlaceholderText(/edad aproximada/), { target: { value: 'xyzxyzxyz' } })
    expect(screen.getByText('Sin resultados para «xyzxyzxyz».')).toBeInTheDocument()
  })

  it('al elegir un resultado navega al capítulo correcto', () => {
    renderSearch()
    fireEvent.change(screen.getByPlaceholderText(/edad aproximada/), { target: { value: 'edad aproximada' } })

    const compraOption = screen.getAllByRole('button').find((button) => button.textContent?.includes('Compras'))!
    fireEvent.mouseDown(compraOption)

    expect(screen.getByText('Página de destino')).toBeInTheDocument()
  })
})
