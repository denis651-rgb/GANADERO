import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { AnimalDetailPage } from './AnimalDetailPage'

const changeAnimalState = vi.fn()
vi.mock('@/features/animales/api', () => ({
  changeAnimalState: (...args: unknown[]) => changeAnimalState(...args),
  getAnimal: vi.fn().mockResolvedValue({ id: 'a-1', codigo: 'A-001', nombre: 'Luna', estado: 'ACTIVO', sexo: 'HEMBRA', version: 3 }),
  getAnimalTimeline: vi.fn().mockResolvedValue({ content: [], page: 0, totalPages: 0 }),
  listCategorias: vi.fn().mockResolvedValue([]), listRazas: vi.fn().mockResolvedValue([]),
}))
vi.mock('@/features/propiedades/api', () => ({ listPropiedades: vi.fn().mockResolvedValue([]) }))
vi.mock('@/features/potreros/api', () => ({ listPotreros: vi.fn().mockResolvedValue([]) }))
vi.mock('@/features/animales/components/GenealogiaTab', () => ({ GenealogiaTab: () => null }))
vi.mock('@/features/animales/components/IdentificadoresTab', () => ({ IdentificadoresTab: () => null }))
vi.mock('@/features/animales/components/FotosTab', () => ({ FotosTab: () => null }))
vi.mock('@/shared/toast/useToast', () => ({ useToast: () => ({ showToast: vi.fn() }) }))

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(<QueryClientProvider client={client}><MemoryRouter initialEntries={['/animales/a-1']}><Routes><Route path="/animales/:id" element={<AnimalDetailPage />} /></Routes></MemoryRouter></QueryClientProvider>)
}

describe('AnimalDetailPage state protection', () => {
  it('no cambia un estado crítico hasta confirmarlo y cancelar no llama la API', async () => {
    changeAnimalState.mockReset().mockResolvedValue({})
    renderPage()
    await screen.findByText('A-001 · Luna')
    fireEvent.change(screen.getByRole('combobox', { name: '' }), { target: { value: 'MUERTO' } })
    fireEvent.change(screen.getByPlaceholderText('Motivo del cambio…'), { target: { value: 'Registro confirmado' } })
    fireEvent.click(screen.getByRole('button', { name: 'Actualizar estado' }))

    expect(await screen.findByRole('dialog', { name: 'Confirmar cambio de estado' })).toBeInTheDocument()
    expect(changeAnimalState).not.toHaveBeenCalled()
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }))
    expect(changeAnimalState).not.toHaveBeenCalled()

    fireEvent.click(screen.getByRole('button', { name: 'Actualizar estado' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Confirmar cambio' }))
    await waitFor(() => expect(changeAnimalState).toHaveBeenCalledOnce())
  })

  it('implementa roving tabindex y navegación completa por teclado en tabs', async () => {
    renderPage()
    const timeline = await screen.findByRole('tab', { name: 'Línea de tiempo' })
    const identifiers = screen.getByRole('tab', { name: 'Identificadores' })
    const photos = screen.getByRole('tab', { name: 'Fotografías' })
    const genealogy = screen.getByRole('tab', { name: 'Genealogía' })
    expect(timeline).toHaveAttribute('aria-selected', 'true')
    expect(timeline).toHaveAttribute('tabindex', '0')
    expect(identifiers).toHaveAttribute('tabindex', '-1')

    timeline.focus()
    fireEvent.keyDown(timeline, { key: 'ArrowRight' })
    expect(identifiers).toHaveFocus()
    expect(identifiers).toHaveAttribute('aria-selected', 'true')
    fireEvent.keyDown(identifiers, { key: 'End' })
    expect(genealogy).toHaveFocus()
    fireEvent.keyDown(genealogy, { key: 'Home' })
    expect(timeline).toHaveFocus()
    fireEvent.keyDown(timeline, { key: 'ArrowLeft' })
    expect(genealogy).toHaveFocus()
    fireEvent.keyDown(genealogy, { key: 'ArrowLeft' })
    expect(photos).toHaveFocus()
  })
})
