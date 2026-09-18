import { useDeferredValue, useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Check, Search } from 'lucide-react'
import { listAnimals } from '@/features/animales/api'
import type { AnimalSummary } from '@/features/animales/types'
import type { Propiedad } from '@/features/propiedades/api'
import type { Potrero } from '@/features/potreros/api'
import type { Lote } from '@/features/lotes/api'
import { Field } from '@/shared/components/Field'

interface AnimalPickerProps {
  value?: AnimalSummary | null
  onChange: (animal: AnimalSummary) => void
  error?: string
  properties?: Propiedad[]
  paddocks?: Potrero[]
  lots?: Lote[]
}

export function AnimalPicker({ value, onChange, error, properties, paddocks, lots }: AnimalPickerProps) {
  const [search, setSearch] = useState('')
  const [open, setOpen] = useState(false)
  const [propiedadId, setPropiedadId] = useState('')
  const [potreroId, setPotreroId] = useState('')
  const [loteId, setLoteId] = useState('')
  const deferred = useDeferredValue(search.trim())
  const query = useQuery({
    queryKey: ['pesaje-animal-search', deferred, propiedadId, potreroId, loteId],
    queryFn: () => listAnimals({
      search: deferred, estado: 'ACTIVO', page: 0, size: 8,
      propiedadId: propiedadId || undefined, potreroId: potreroId || undefined, loteId: loteId || undefined,
    }),
    // Con una propiedad elegida, se puede recorrer su hato sin escribir nada; sin filtro, se
    // necesita texto para no traer el listado completo de animales de la empresa.
    enabled: deferred.length >= 2 || Boolean(propiedadId),
    placeholderData: keepPreviousData,
  })

  const results = query.data?.content

  function cambiarPropiedad(id: string) {
    setPropiedadId(id)
    setPotreroId('')
    setLoteId('')
  }

  return (
    <div className="animal-picker-row">
      <div className="picker-root">
        <Field
          label="Animal"
          error={error}
          icon={<Search size={18} />}
          hint={value ? undefined : 'Escribe al menos 2 caracteres, o elige una propiedad para ver su hato.'}
        >
          <input
            value={search}
            onChange={(event) => { setSearch(event.target.value); setOpen(true) }}
            onFocus={() => setOpen(true)}
            onBlur={() => setOpen(false)}
            placeholder={value ? `${value.codigo}${value.nombre ? ` · ${value.nombre}` : ''}` : 'Buscar animal…'}
          />
        </Field>
        {open && (deferred.length >= 2 || propiedadId) && (
          <div className="picker-results">
            {!results && <div className="picker-empty">Buscando…</div>}
            {results && results.length === 0 && <div className="picker-empty">Sin resultados.</div>}
            {results?.map((animal) => (
              <button
                key={animal.id}
                type="button"
                className="picker-option"
                onMouseDown={(event) => { event.preventDefault(); onChange(animal); setSearch(''); setOpen(false) }}
              >
                <span><strong>{animal.codigo}</strong>{animal.nombre ? ` · ${animal.nombre}` : ''}</span>
                {value?.id === animal.id && <Check size={16} />}
              </button>
            ))}
          </div>
        )}
      </div>
      <Field label="Propiedad">
        <select value={propiedadId} onChange={(event) => cambiarPropiedad(event.target.value)}>
          <option value="">Todas</option>
          {properties?.filter((item) => item.activo).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}
        </select>
      </Field>
      <Field label="Potrero" hint={propiedadId ? undefined : 'Elige una propiedad primero.'}>
        <select value={potreroId} disabled={!propiedadId} onChange={(event) => { setPotreroId(event.target.value); setLoteId('') }}>
          <option value="">Todos</option>
          {paddocks?.filter((item) => item.activo && item.propiedadId === propiedadId).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}
        </select>
      </Field>
      <Field label="Lote" hint={propiedadId ? undefined : 'Elige una propiedad primero.'}>
        <select value={loteId} disabled={!propiedadId} onChange={(event) => setLoteId(event.target.value)}>
          <option value="">Todos</option>
          {lots?.filter((item) => item.estado === 'ACTIVO' && item.propiedadId === propiedadId).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}
        </select>
      </Field>
    </div>
  )
}
