import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { AppError } from '@/shared/api/errors'
import { SectorEditModal } from './SectorEditModal'

const sector = { id: 's-1', propiedadId: 'p-1', codigo: 'NORTE', nombre: 'Norte', descripcion: 'Inicial', activo: true, version: 4 }

describe('SectorEditModal', () => {
  it('envía los datos editados con la versión vigente', () => {
    const onSubmit = vi.fn()
    render(<SectorEditModal sector={sector} online loading={false} error={null} onClose={vi.fn()} onSubmit={onSubmit} onReload={vi.fn()} />)

    fireEvent.change(screen.getByLabelText(/^Nombre/), { target: { value: 'Sector norte' } })
    fireEvent.click(screen.getByRole('button', { name: 'Guardar sector' }))

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ codigo: 'NORTE', nombre: 'Sector norte', activo: true, version: 4 }))
  })

  it('bloquea el guardado y permite recargar ante un conflicto de versión', () => {
    const onReload = vi.fn()
    render(<SectorEditModal sector={sector} online loading={false} error={new AppError('Conflicto', { code: 'VERSION_CONFLICT' })} onClose={vi.fn()} onSubmit={vi.fn()} onReload={onReload} />)

    expect(screen.getByRole('button', { name: 'Guardar sector' })).toBeDisabled()
    fireEvent.click(screen.getByRole('button', { name: 'Recargar datos' }))
    expect(onReload).toHaveBeenCalledOnce()
  })
})
