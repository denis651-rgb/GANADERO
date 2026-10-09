import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { GenealogiaTab } from './GenealogiaTab'
import { crearParentesco, listAnimals, listCategorias, listParentescos, listRazas } from '@/features/animales/api'
import { listPropiedades } from '@/features/propiedades/api'
import { listAllPotreros } from '@/features/potreros/api'

vi.mock('@/features/animales/api', () => ({
  listParentescos: vi.fn(),
  listRazas: vi.fn(),
  listCategorias: vi.fn(),
  listAnimals: vi.fn(),
  crearParentesco: vi.fn(),
  eliminarParentesco: vi.fn(),
}))

vi.mock('@/features/propiedades/api', () => ({
  listPropiedades: vi.fn(),
}))

vi.mock('@/features/potreros/api', () => ({
  listAllPotreros: vi.fn(),
}))

vi.mock('@/shared/toast/useToast', () => ({
  useToast: () => ({ showToast: vi.fn() }),
}))

function renderComponent(animalId = 'a-current') {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <GenealogiaTab animalId={animalId} />
    </QueryClientProvider>
  )
}

describe('GenealogiaTab', () => {
  it('filtra hembras cuando el tipo es Madre y machos cuando es Padre, con descripciones completas', async () => {
    vi.mocked(listParentescos).mockResolvedValue([])
    vi.mocked(listRazas).mockResolvedValue([
      { id: 'r-1', codigo: 'NEL', nombre: 'Nelore', especie: 'BOVINO' },
      { id: 'r-2', codigo: 'BRAH', nombre: 'Brahman', especie: 'BOVINO' },
    ])
    vi.mocked(listCategorias).mockResolvedValue([
      { id: 'c-vaca', codigo: 'VACA', nombre: 'Vaca', sexoAplicable: 'HEMBRA', edadMinMeses: 36, activo: true, clasificacionAutomatica: true, ordenEvaluacion: 0 },
      { id: 'c-toro', codigo: 'TORO', nombre: 'Toro', sexoAplicable: 'MACHO', edadMinMeses: 36, activo: true, clasificacionAutomatica: true, ordenEvaluacion: 0 },
    ])
    vi.mocked(listPropiedades).mockResolvedValue([
      { id: 'p-1', codigo: 'CV', nombre: 'Cerro Verde', activo: true },
    ])
    vi.mocked(listAllPotreros).mockResolvedValue([
      { id: 'pot-1', propiedadId: 'p-1', nombre: '3ra Division', activo: true },
      { id: 'pot-2', propiedadId: 'p-1', nombre: 'Corral', activo: true },
    ])
    vi.mocked(listAnimals).mockResolvedValue({
      content: [
        { id: 'a-current', codigo: 'ANI-000001', nombre: 'Cria', sexo: 'HEMBRA', estado: 'ACTIVO', razaPrincipalId: 'r-1', categoriaActualId: 'c-vaca', propiedadActualId: 'p-1', potreroActualId: 'pot-1', version: 1 } as any,
        { id: 'a-madre1', codigo: 'ANI-000018', nombre: 'LVN-30', sexo: 'HEMBRA', estado: 'ACTIVO', razaPrincipalId: 'r-1', categoriaActualId: 'c-vaca', propiedadActualId: 'p-1', potreroActualId: 'pot-1', version: 1 } as any,
        { id: 'a-padre1', codigo: 'ANI-000099', nombre: 'Toro Bravo', sexo: 'MACHO', estado: 'ACTIVO', razaPrincipalId: 'r-2', categoriaActualId: 'c-toro', propiedadActualId: 'p-1', potreroActualId: 'pot-2', version: 1 } as any,
      ],
      page: 0, size: 1000, totalElements: 3, totalPages: 1,
    })

    renderComponent('a-current')

    // Abrir formulario de registro de progenitor
    fireEvent.click(await screen.findByRole('button', { name: 'Registrar progenitor' }))

    // Por defecto es Madre -> al enfocar o escribir en el buscador, muestra solo las hembras elegibles
    const inputMadre = screen.getByRole('combobox', { name: /Animal progenitor \(Hembra\)/ })
    fireEvent.focus(inputMadre)

    expect(screen.getByText('LVN-30 (ANI-000018)')).toBeInTheDocument()
    expect(screen.getByText(/Nelore · Vaca · Cerro Verde \/ 3ra Division/)).toBeInTheDocument()
    expect(screen.queryByText('Toro Bravo (ANI-000099)')).not.toBeInTheDocument()
    expect(screen.queryByText('Cria (ANI-000001)')).not.toBeInTheDocument()

    // Cambiar tipo de progenitor a Padre
    const selectTipo = screen.getByRole('combobox', { name: 'Tipo de progenitor' })
    fireEvent.change(selectTipo, { target: { value: 'PADRE' } })

    // Ahora al enfocar debe mostrar solo al macho a-padre1
    const inputPadre = screen.getByRole('combobox', { name: /Animal progenitor \(Macho\)/ })
    fireEvent.focus(inputPadre)

    expect(screen.getByText('Toro Bravo (ANI-000099)')).toBeInTheDocument()
    expect(screen.getByText(/Brahman · Toro · Cerro Verde \/ Corral/)).toBeInTheDocument()
    expect(screen.queryByText('LVN-30 (ANI-000018)')).not.toBeInTheDocument()

    // Probar el filtrado en vivo mientras se escribe
    fireEvent.change(inputPadre, { target: { value: 'Bravo' } })
    expect(screen.getByText('Toro Bravo (ANI-000099)')).toBeInTheDocument()

    fireEvent.change(inputPadre, { target: { value: 'Inexistente' } })
    expect(screen.getByText(/No se encontró ningún animal de sexo macho/)).toBeInTheDocument()

    // Seleccionar al macho
    fireEvent.change(inputPadre, { target: { value: 'Bravo' } })
    const optionToro = screen.getByRole('button', { name: /Toro Bravo/ })
    fireEvent.mouseDown(optionToro)

    // Al seleccionar, se muestra el animal seleccionado y el botón Cambiar
    expect(screen.getByRole('button', { name: 'Cambiar' })).toBeInTheDocument()
    expect(screen.getByDisplayValue(/Toro Bravo \(ANI-000099\) · \[Brahman · Toro · Cerro Verde \/ Corral\]/)).toBeInTheDocument()
  })
})

