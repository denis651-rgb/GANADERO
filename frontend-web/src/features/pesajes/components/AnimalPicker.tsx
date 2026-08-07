import { useDeferredValue, useEffect, useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Check, Search } from 'lucide-react'
import { listAnimals } from '@/features/animales/api'
import type { AnimalSummary } from '@/features/animales/types'
import { searchLocalAnimals, type LocalAnimalResult } from '@/offline/catalogs'
import { Field } from '@/shared/components/Field'

function toSummary(local: LocalAnimalResult): AnimalSummary {
  return {
    id: local.id,
    codigo: local.codigo,
    nombre: local.nombre,
    sexo: 'HEMBRA',
    categoriaActualId: '',
    fechaNacimientoEstimada: false,
    razaPrincipalId: '',
    proposito: 'CARNE',
    origen: 'NACIDO',
    estado: 'ACTIVO',
    propiedadActualId: '',
    potreroActualId: '',
    fechaIngreso: new Date().toISOString().slice(0, 10),
    version: 1,
  } as AnimalSummary
}

interface AnimalPickerProps {
  value?: AnimalSummary | null
  onChange: (animal: AnimalSummary) => void
  error?: string
}

export function AnimalPicker({ value, onChange, error }: AnimalPickerProps) {
  const [search, setSearch] = useState('')
  const [open, setOpen] = useState(false)
  const [localResults, setLocalResults] = useState<AnimalSummary[]>([])
  const deferred = useDeferredValue(search.trim())
  const offline = !navigator.onLine
  const query = useQuery({
    queryKey: ['pesaje-animal-search', deferred],
    queryFn: () => listAnimals({ search: deferred, estado: 'ACTIVO', page: 0, size: 8 }),
    enabled: deferred.length >= 2 && !offline,
    placeholderData: keepPreviousData,
  })

  useEffect(() => {
    if (deferred.length < 2 || !offline) return
    let active = true
    void searchLocalAnimals(deferred).then((rows) => {
      if (!active) return
      setLocalResults(rows.map(toSummary))
    })
    return () => {
      active = false
    }
  }, [deferred, offline])

  const results = offline ? localResults : query.data?.content

  return (
    <>
      <Field
        label="Animal"
        error={error}
        icon={<Search size={18} />}
        hint={value ? undefined : 'Escribe al menos 2 caracteres para buscar por código o nombre.'}
      >
        <input
          value={search}
          onChange={(event) => { setSearch(event.target.value); setOpen(true) }}
          onFocus={() => setOpen(true)}
          onBlur={() => setOpen(false)}
          placeholder={value ? `${value.codigo}${value.nombre ? ` · ${value.nombre}` : ''}` : 'Buscar animal…'}
        />
      </Field>
      {open && search.trim().length >= 2 && (
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
    </>
  )
}
