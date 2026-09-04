import { useEffect, useId, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { getAnimal, listAnimals, listRazas } from '@/features/animales/api'
import type { AnimalSummary } from '@/features/animales/types'
import { Field } from '@/shared/components/Field'

export function AnimalSearchSelect({ label, name, sexo, value, onChange }: {
  label: string; name: string; sexo?: 'HEMBRA' | 'MACHO'; value?: string
  onChange?: (id: string, animal?: AnimalSummary) => void
}) {
  const id = useId()
  const input = useRef<HTMLInputElement>(null)
  const [search, setSearch] = useState('')
  const [term, setTerm] = useState('')
  const [selected, setSelected] = useState<AnimalSummary>()
  const [open, setOpen] = useState(false)
  const [active, setActive] = useState(-1)
  const selectedId = value ?? selected?.id ?? ''
  const saved = useQuery({ queryKey: ['animal', selectedId], queryFn: () => getAnimal(selectedId), enabled: !!selectedId && selected?.id !== selectedId })
  const current = selected?.id === selectedId ? selected : saved.data
  useEffect(() => { const timer = setTimeout(() => setTerm(search.trim()), 300); return () => clearTimeout(timer) }, [search])
  useEffect(() => { input.current?.setCustomValidity(selectedId ? '' : 'Selecciona un animal de los resultados.') }, [selectedId, search])
  const animals = useQuery({ queryKey: ['reproduccion-buscar-animal', sexo, term], queryFn: () => listAnimals({ sexo, estado: 'ACTIVO', search: term, page: 0, size: 50 }), enabled: open })
  const razas = useQuery({ queryKey: ['razas'], queryFn: listRazas, staleTime: 300_000 })
  const results = search.trim() === term && !animals.isFetching ? animals.data?.content ?? [] : []
  const breed = (animal: AnimalSummary) => razas.data?.find((raza) => raza.id === animal.razaPrincipalId)?.nombre || 'Raza no registrada'
  const choose = (animal: AnimalSummary) => {
    setSelected(animal); onChange?.(animal.id, animal); setSearch(''); setTerm(''); setOpen(false); setActive(-1)
  }
  return <div style={{ position: 'relative' }} onBlur={(event) => { if (!event.currentTarget.contains(event.relatedTarget)) setOpen(false) }}>
    <input type="hidden" name={name} value={selectedId} />
    <Field label={label} required><input ref={input} role="combobox" autoComplete="off" aria-autocomplete="list" aria-expanded={open} aria-controls={id} aria-activedescendant={open && results[active] ? id + '-' + active : undefined}
      value={selectedId && current ? (current.nombre || 'Sin nombre') + ' · ' + current.codigo + ' · ' + breed(current) : search} placeholder="Buscar por arete, nombre o raza…"
      onFocus={() => setOpen(true)} onClick={() => setOpen(true)}
      onChange={(event) => { setSearch(event.target.value); setSelected(undefined); onChange?.(''); setOpen(true); setActive(-1) }}
      onKeyDown={(event) => {
        if (event.key === 'Escape' && open) { event.preventDefault(); event.stopPropagation(); setOpen(false) }
        if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
          event.preventDefault(); setOpen(true)
          setActive((previous) => results.length ? (previous + (event.key === 'ArrowDown' ? 1 : -1) + results.length) % results.length : -1)
        }
        if (event.key === 'Enter' && open) { event.preventDefault(); if (results[active]) choose(results[active]) }
      }} /></Field>
    {open && <div style={{ position: 'absolute', top: '100%', left: 0, right: 0, zIndex: 20, background: 'white', border: '1px solid #cbd8d0', borderRadius: 8, boxShadow: '0 8px 24px #0002', maxHeight: 240, overflowY: 'auto' }}>
      <div id={id} role="listbox" aria-label={'Resultados de ' + label.toLowerCase()}>
        {results.map((animal, index) => <div key={animal.id} id={id + '-' + index} role="option" aria-selected={selectedId === animal.id} onMouseDown={(event) => event.preventDefault()} onClick={() => choose(animal)} onMouseEnter={() => setActive(index)} style={{ padding: 12, cursor: 'pointer', background: active === index ? '#e9f4ed' : undefined }}>
          <strong>{animal.nombre || 'Sin nombre'}</strong><span className="table-secondary">{animal.codigo} · {breed(animal)}</span>
        </div>)}
      </div>
      <small role="status" style={{ display: 'block', padding: 8 }}>{animals.isFetching || search.trim() !== term ? 'Buscando…' : animals.isError ? 'No se pudo buscar. Intenta nuevamente.' : !results.length ? 'Sin animales que coincidan.' : animals.data && animals.data.totalElements > 50 ? 'Escribe más para precisar los resultados.' : 'Selecciona un animal de la lista.'}</small>
    </div>}
  </div>
}
