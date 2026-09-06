import { useDeferredValue, useState, type FormEvent } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Check, Search, UserPlus, X } from 'lucide-react'
import { buscarProveedores } from '@/features/proveedores/api'
import type { ProveedorNuevoInput, ProveedorSeleccion } from '@/features/proveedores/types'
import { Field } from '@/shared/components/Field'
import { Button } from '@/shared/components/Button'
import { Modal } from '@/shared/components/Modal'

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
  const [nuevo, setNuevo] = useState<ProveedorNuevoInput>({ nombre: '' })
  const deferred = useDeferredValue(search.trim())
  const query = useQuery({
    queryKey: ['proveedor-search', deferred],
    queryFn: () => buscarProveedores(deferred, true),
    enabled: deferred.length >= 2 && !modoNuevo,
    placeholderData: keepPreviousData,
  })
  const results = query.data

  function abrirNuevo() {
    setNuevo(value.proveedorNuevo ?? { nombre: '' })
    setOpen(false)
    setModoNuevo(true)
  }

  function guardarNuevo(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    event.stopPropagation()
    const nombre = nuevo.nombre.trim()
    if (!nombre) return
    onChange({ proveedorNuevo: { ...nuevo, nombre }, etiqueta: nombre })
    setSearch('')
    setModoNuevo(false)
  }

  if (value.proveedorId || value.proveedorNuevo) {
    const etiqueta = value.etiqueta ?? value.proveedorNuevo?.nombre ?? 'Proveedor seleccionado'
    return (
      <Field label="Proveedor" error={error}>
        <div className="picker-selected">
          <input value={value.proveedorNuevo ? `${etiqueta} · Nuevo` : etiqueta} readOnly />
          <Button type="button" variant="ghost" onClick={() => onChange({})}><X size={16} aria-hidden="true" />Cambiar</Button>
        </div>
      </Field>
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
      <div className="form-full"><Button type="button" variant="secondary" onClick={abrirNuevo}><UserPlus size={16} aria-hidden="true" />Registrar proveedor nuevo</Button></div>
      <Modal open={modoNuevo} title="Registrar proveedor nuevo" description="Completa los datos del proveedor para asociarlo a esta compra." onClose={() => setModoNuevo(false)}>
        <form className="form-grid" onSubmit={guardarNuevo}>
          <Field label="Nombre" required>
            <input required autoFocus value={nuevo.nombre} onChange={(event) => setNuevo((actual) => ({ ...actual, nombre: event.target.value }))} maxLength={160} placeholder="Ej. Estancia El Roble…" />
          </Field>
          <Field label="Teléfono"><input value={nuevo.telefono ?? ''} onChange={(event) => setNuevo((actual) => ({ ...actual, telefono: event.target.value || undefined }))} maxLength={30} placeholder="Ej. 76543210…" /></Field>
          <Field label="Documento / NIT" hint="Si ya existe un proveedor con este documento, el sistema reutilizará ese registro.">
            <input value={nuevo.documento ?? ''} onChange={(event) => setNuevo((actual) => ({ ...actual, documento: event.target.value || undefined }))} maxLength={30} placeholder="Documento o NIT…" />
          </Field>
          <Field label="Dirección"><input value={nuevo.direccion ?? ''} onChange={(event) => setNuevo((actual) => ({ ...actual, direccion: event.target.value || undefined }))} maxLength={200} placeholder="Dirección del proveedor…" /></Field>
          <Field label="Correo"><input type="email" value={nuevo.correo ?? ''} onChange={(event) => setNuevo((actual) => ({ ...actual, correo: event.target.value || undefined }))} maxLength={160} placeholder="correo@ejemplo.com…" /></Field>
          <div className="form-full form-actions">
            <Button type="button" variant="secondary" onClick={() => setModoNuevo(false)}>Cancelar</Button>
            <Button type="submit"><UserPlus size={17} aria-hidden="true" />Guardar proveedor</Button>
          </div>
        </form>
      </Modal>
    </>
  )
}
