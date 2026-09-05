import { useDeferredValue, useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Check, Search, UserPlus, X } from 'lucide-react'
import { buscarProveedores } from '@/features/proveedores/api'
import type { ProveedorSeleccion } from '@/features/proveedores/types'
import { Field } from '@/shared/components/Field'
import { Button } from '@/shared/components/Button'

interface ProveedorPickerProps {
  value: ProveedorSeleccion
  onChange: (value: ProveedorSeleccion) => void
  error?: string
}

/**
 * Busca un proveedor existente o prepara los datos de uno nuevo. No crea nada por sí mismo:
 * el proveedor nuevo se resuelve (buscar-o-crear, sin duplicar por documento) recién al
 * confirmar la compra, en el backend.
 */
export function ProveedorPicker({ value, onChange, error }: ProveedorPickerProps) {
  const [search, setSearch] = useState('')
  const [open, setOpen] = useState(false)
  const [modoNuevo, setModoNuevo] = useState(false)
  const deferred = useDeferredValue(search.trim())
  const query = useQuery({
    queryKey: ['proveedor-search', deferred],
    queryFn: () => buscarProveedores(deferred, true),
    enabled: deferred.length >= 2 && !modoNuevo,
    placeholderData: keepPreviousData,
  })
  const results = query.data

  if (value.proveedorId) {
    return (
      <Field label="Proveedor" error={error}>
        <div className="picker-selected">
          <input value={value.etiqueta ?? 'Proveedor seleccionado'} readOnly />
          <Button type="button" variant="ghost" onClick={() => onChange({})}><X size={16} aria-hidden="true" />Cambiar</Button>
        </div>
      </Field>
    )
  }

  if (modoNuevo || value.proveedorNuevo) {
    const nuevo = value.proveedorNuevo ?? { nombre: '' }
    const setNuevo = (patch: Partial<typeof nuevo>) => onChange({ proveedorNuevo: { ...nuevo, ...patch } })
    return (
      <div className="form-full">
        <div className="section-heading">
          <span className="eyebrow">Proveedor nuevo</span>
          <Button type="button" variant="ghost" onClick={() => { setModoNuevo(false); onChange({}) }}><X size={16} aria-hidden="true" />Buscar existente</Button>
        </div>
        <div className="form-grid">
          <Field label="Nombre" error={!nuevo.nombre ? error : undefined}>
            <input value={nuevo.nombre} onChange={(event) => setNuevo({ nombre: event.target.value })} maxLength={160} placeholder="Estancia El Roble" />
          </Field>
          <Field label="Teléfono"><input value={nuevo.telefono ?? ''} onChange={(event) => setNuevo({ telefono: event.target.value || undefined })} maxLength={30} /></Field>
          <Field label="Documento / NIT" hint="Evita duplicados: si ya existe un proveedor con este documento, se reutiliza.">
            <input value={nuevo.documento ?? ''} onChange={(event) => setNuevo({ documento: event.target.value || undefined })} maxLength={30} />
          </Field>
          <Field label="Dirección"><input value={nuevo.direccion ?? ''} onChange={(event) => setNuevo({ direccion: event.target.value || undefined })} maxLength={200} /></Field>
          <Field label="Correo"><input type="email" value={nuevo.correo ?? ''} onChange={(event) => setNuevo({ correo: event.target.value || undefined })} maxLength={160} /></Field>
        </div>
      </div>
    )
  }

  return (
    <>
      <Field label="Proveedor" error={error} icon={<Search size={18} aria-hidden="true" />} hint="Escribe al menos 2 caracteres para buscar por nombre, teléfono o documento.">
        <input
          value={search}
          onChange={(event) => { setSearch(event.target.value); setOpen(true) }}
          onFocus={() => setOpen(true)}
          onBlur={() => setTimeout(() => setOpen(false), 150)}
          placeholder="Buscar proveedor…"
        />
      </Field>
      {open && search.trim().length >= 2 && (
        <div className="picker-results">
          {!results && <div className="picker-empty">Buscando…</div>}
          {results && results.length === 0 && <div className="picker-empty">Sin resultados.</div>}
          {results?.map((proveedor) => (
            <button
              key={proveedor.id}
              type="button"
              className="picker-option"
              onMouseDown={(event) => {
                event.preventDefault()
                onChange({ proveedorId: proveedor.id, etiqueta: proveedor.nombre })
                setSearch('')
                setOpen(false)
              }}
            >
              <span><strong>{proveedor.nombre}</strong>{proveedor.telefono ? ` · ${proveedor.telefono}` : ''}</span>
              <Check size={16} aria-hidden="true" style={{ visibility: 'hidden' }} />
            </button>
          ))}
        </div>
      )}
      <div className="form-full"><Button type="button" variant="secondary" onClick={() => setModoNuevo(true)}><UserPlus size={16} aria-hidden="true" />Registrar proveedor nuevo</Button></div>
    </>
  )
}
