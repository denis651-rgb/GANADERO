import { useMemo, useState } from 'react'
import { Search } from 'lucide-react'
import type { AnimalSummary } from '@/features/animales/types'

interface AnimalPickerProps {
  animales: AnimalSummary[]
  cargando: boolean
  value: string
  onChange: (id: string) => void
}

const LIMITE_RESULTADOS = 40

/**
 * Selección de un animal para la venta individual: un buscador por código (arete) o nombre que
 * filtra en vivo la lista de abajo, donde se elige un animal con un click. Mismo patrón visual
 * (fieldset + lista filtrable) que AnimalMultiPicker, pero de una sola selección.
 */
export function AnimalPicker({ animales, cargando, value, onChange }: AnimalPickerProps) {
  const [search, setSearch] = useState('')

  const filtro = search.trim().toLowerCase()
  const filtrados = useMemo(() => {
    const lista = filtro
      ? animales.filter((a) => a.codigo.toLowerCase().includes(filtro) || (a.nombre ?? '').toLowerCase().includes(filtro))
      : animales
    return lista.slice(0, LIMITE_RESULTADOS)
  }, [animales, filtro])

  return (
    <fieldset className="movement-animal-picker form-full">
      <legend>Animal</legend>
      <div className="movement-picker-toolbar">
        <span className="search-box">
          <Search size={18} aria-hidden="true" />
          <input type="search" autoComplete="off" aria-label="Buscar animal por código o nombre" value={search}
            disabled={cargando} onChange={(event) => setSearch(event.target.value)} placeholder="Buscar por código (arete) o nombre…" />
        </span>
      </div>
      {cargando
        ? <p className="movement-picker-empty" role="status">Cargando animales…</p>
        : filtrados.length > 0
          ? <div className="movement-animal-list">{filtrados.map((animal) => (
              <label key={animal.id} className="movement-animal-option">
                <input type="radio" name="animalVenta" checked={value === animal.id} onChange={() => onChange(animal.id)} />
                <span><strong>{animal.codigo}</strong>{animal.nombre ? ` · ${animal.nombre}` : ''}</span>
              </label>
            ))}</div>
          : <p className="movement-picker-empty" role="status">No hay animales activos que coincidan con la búsqueda.</p>}
    </fieldset>
  )
}
