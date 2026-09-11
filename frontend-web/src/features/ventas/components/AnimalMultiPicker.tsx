import { useDeferredValue, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Group, Search } from 'lucide-react'
import { listAnimals } from '@/features/animales/api'
import { listLotes, listMembresias } from '@/features/lotes/api'
import type { AnimalSummary } from '@/features/animales/types'
import { Field } from '@/shared/components/Field'

interface AnimalMultiPickerProps {
  animales: AnimalSummary[]
  cargando: boolean
  seleccionados: Set<string>
  onChange: (next: Set<string>) => void
}

/**
 * Selección múltiple de animales activos para una venta por lote: sin búsqueda, muestra la lista ya
 * cargada por la página; al escribir (2+ caracteres) delega la búsqueda al backend (código, nombre,
 * arete/identificador y raza — igual que el resto de la app) en vez de filtrar solo por código/nombre
 * en memoria. Además, un buscador de lotes agrega de una vez a todos sus animales activos (vía
 * listMembresias) sin requerir un endpoint nuevo en el backend.
 */
export function AnimalMultiPicker({ animales, cargando, seleccionados, onChange }: AnimalMultiPickerProps) {
  const [search, setSearch] = useState('')
  const [loteSearch, setLoteSearch] = useState('')
  const [loteOpen, setLoteOpen] = useState(false)
  const [cargandoLote, setCargandoLote] = useState(false)

  const deferredSearch = useDeferredValue(search.trim())
  const busqueda = useQuery({
    queryKey: ['venta-animal-picker', deferredSearch],
    queryFn: () => listAnimals({ search: deferredSearch, estado: 'ACTIVO', sexo: '', page: 0, size: 200 }),
    enabled: deferredSearch.length >= 2,
  })
  const buscando = deferredSearch.length >= 2
  const filtrados = buscando ? (busqueda.data?.content ?? []) : animales

  const loteDeferred = useDeferredValue(loteSearch.trim())
  const loteQuery = useQuery({
    queryKey: ['venta-lote-picker', loteDeferred],
    queryFn: () => listLotes({ estado: 'ACTIVO', search: loteDeferred, page: 0, size: 20 }),
    enabled: loteDeferred.length >= 2,
  })

  function alternar(id: string) {
    const next = new Set(seleccionados)
    if (next.has(id)) next.delete(id)
    else next.add(id)
    onChange(next)
  }

  async function agregarLoteCompleto(loteId: string) {
    setCargandoLote(true)
    try {
      const miembros = await listMembresias(loteId, true)
      const next = new Set(seleccionados)
      miembros.forEach((m) => next.add(m.animalId))
      onChange(next)
      setLoteSearch('')
      setLoteOpen(false)
    } finally {
      setCargandoLote(false)
    }
  }

  return (
    <>
      <fieldset className="movement-animal-picker form-full">
        <legend>Animales a vender</legend>
        <div className="movement-picker-toolbar">
          <span className="search-box">
            <Search size={18} aria-hidden="true" />
            <input type="search" autoComplete="off" aria-label="Buscar animales por código, arete, nombre o raza" value={search}
              disabled={cargando} onChange={(event) => setSearch(event.target.value)} placeholder="Buscar por código, arete, nombre o raza…" />
          </span>
          <span className="movement-selection-summary" aria-live="polite">{seleccionados.size} seleccionados · {filtrados.length} disponibles</span>
        </div>
        {(cargando && !buscando) || (buscando && busqueda.isPending)
          ? <p className="movement-picker-empty" role="status">{buscando ? 'Buscando…' : 'Cargando animales…'}</p>
          : filtrados.length > 0
            ? <div className="movement-animal-list">{filtrados.map((animal) => (
                <label key={animal.id} className="movement-animal-option">
                  <input type="checkbox" checked={seleccionados.has(animal.id)} onChange={() => alternar(animal.id)} />
                  <span><strong>{animal.codigo}</strong>{animal.nombre ? ` · ${animal.nombre}` : ''}</span>
                </label>
              ))}</div>
            : <p className="movement-picker-empty" role="status">No hay animales activos que coincidan con la búsqueda.</p>}
      </fieldset>

      <div className="form-full picker-root">
        <Field label="…o vender un lote completo" icon={<Group size={18} aria-hidden="true" />}
          hint="Busca un lote activo; se agregan a la selección todos sus animales activos.">
          <input
            value={loteSearch}
            disabled={cargandoLote}
            onChange={(event) => { setLoteSearch(event.target.value); setLoteOpen(true) }}
            onFocus={() => setLoteOpen(true)}
            onBlur={() => setTimeout(() => setLoteOpen(false), 150)}
            placeholder="Ej. Lote Potrero Norte…"
          />
        </Field>
        {loteOpen && loteDeferred.length >= 2 && (
          <div className="picker-results">
            {loteQuery.isPending && <div className="picker-empty">Buscando…</div>}
            {loteQuery.data && loteQuery.data.content.length === 0 && <div className="picker-empty">Sin lotes activos que coincidan.</div>}
            {loteQuery.data?.content.map((lote) => (
              <button key={lote.id} type="button" className="picker-option"
                onMouseDown={(event) => { event.preventDefault(); agregarLoteCompleto(lote.id) }}>
                <span>{lote.codigo} · {lote.nombre}</span>
                <span>{lote.cantidadActual} cabeza(s)</span>
              </button>
            ))}
          </div>
        )}
      </div>
    </>
  )
}
