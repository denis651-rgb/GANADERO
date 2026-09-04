import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { expect, it, vi } from 'vitest'
import { listAnimals, listRazas } from '@/features/animales/api'
import { AnimalSearchSelect } from './AnimalSearchSelect'
vi.mock('@/features/animales/api', () => ({ listAnimals: vi.fn(), listRazas: vi.fn() }))

it('busca con sexo y estado y envía el animal seleccionado sin confundirlo con el texto', async () => {
 const animal = { id: 'a', nombre: 'Gringo', codigo: 'ANI-2', razaPrincipalId: 'r' }
 vi.mocked(listAnimals).mockResolvedValue({ content: [animal], totalElements: 1 } as Awaited<ReturnType<typeof listAnimals>>)
 vi.mocked(listRazas).mockResolvedValue([{ id: 'r', nombre: 'Nelore', codigo: 'NEL', especie: 'BOVINO' }])
 const change = vi.fn()
 render(<QueryClientProvider client={new QueryClient()}><AnimalSearchSelect label="Macho" name="machoId" sexo="MACHO" onChange={change} /></QueryClientProvider>)
 const input = screen.getByRole('combobox', { name: 'Macho' })
 fireEvent.focus(input)
 expect(await screen.findByRole('option')).toHaveTextContent('Gringo')
 expect(screen.queryByLabelText('Buscar macho')).not.toBeInTheDocument()
 fireEvent.change(input, { target: { value: 'arete-25' } })
 expect(input).toBeInvalid()
 await waitFor(() => expect(listAnimals).toHaveBeenCalledWith(expect.objectContaining({ search: 'arete-25', sexo: 'MACHO', estado: 'ACTIVO' })))
 await screen.findByRole('option')
 fireEvent.keyDown(input, { key: 'ArrowDown' })
 fireEvent.keyDown(input, { key: 'Enter' })
 expect(change).toHaveBeenCalledWith('a', animal)
 expect(input).toHaveValue('Gringo · ANI-2 · Nelore')
 expect(input).toBeValid()
 expect(screen.queryByRole('listbox')).not.toBeInTheDocument()
 fireEvent.change(input, { target: { value: 'otro' } })
 expect(change).toHaveBeenLastCalledWith('')
 expect(input).toBeInvalid()
})
