import { render, screen, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { ManualPage } from './ManualPage'

vi.mock('@/shared/toast/useToast', () => ({ useToast: () => ({ showToast: vi.fn() }) }))

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/manual" element={<ManualPage />} />
        <Route path="/manual/:chapterId" element={<ManualPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('ManualPage', () => {
  it('sin capítulo en la URL muestra la portada', () => {
    renderAt('/manual')
    expect(screen.getByRole('heading', { level: 1, name: 'Manual de usuario de Ganadero' })).toBeInTheDocument()
  })

  it('con un capítulo en la URL muestra ese capítulo y lo resalta en el índice', () => {
    renderAt('/manual/compras')
    expect(screen.getByRole('heading', { level: 1, name: /Compra de ganado/ })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Compras' })).toHaveAttribute('aria-current', 'page')
  })

  it('un id de capítulo inexistente muestra el estado vacío', () => {
    renderAt('/manual/no-existe')
    expect(screen.getByText('Capítulo no encontrado')).toBeInTheDocument()
  })

  it('muestra el enlace al capítulo siguiente', () => {
    const { container } = renderAt('/manual/compras')
    const navigation = container.querySelector<HTMLElement>('.manual-navigation')
    expect(navigation).not.toBeNull()
    expect(within(navigation!).getByText('Lotes').closest('a')).toHaveAttribute('href', '/manual/lotes')
  })

  it('con una ancla en la URL, hace scroll hasta ese encabezado', () => {
    Element.prototype.scrollIntoView = vi.fn()
    renderAt('/manual/compras#compra-por-lote')
    const target = screen.getByRole('heading', { level: 2, name: /Compra por lote/ })
    expect(target.scrollIntoView).toHaveBeenCalledWith({ block: 'start' })
  })
})
