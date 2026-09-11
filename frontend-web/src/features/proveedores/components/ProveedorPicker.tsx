import { useDeferredValue, useEffect, useId, useRef, useState, type FormEvent, type KeyboardEvent } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Check, ChevronDown, Search, UserPlus } from 'lucide-react'
import { buscarProveedores } from '@/features/proveedores/api'
import type { Proveedor, ProveedorNuevoInput, ProveedorSeleccion } from '@/features/proveedores/types'
import { Field } from '@/shared/components/Field'
import { Button } from '@/shared/components/Button'
import { Modal } from '@/shared/components/Modal'

interface ProveedorPickerProps {
  value: ProveedorSeleccion
  onChange: (value: ProveedorSeleccion) => void
  error?: string
}

/**
 * Combobox: al abrirse carga todos los proveedores activos y filtra en memoria
 * por nombre, teléfono o documento. No crea nada por sí mismo: el proveedor nuevo
 * se resuelve (buscar-o-crear, sin duplicar por documento) recién al confirmar la
 * compra, en el backend.
 */
export function ProveedorPicker({ value, onChange, error }: ProveedorPickerProps) {
  const [search, setSearch] = useState('')
  const [open, setOpen] = useState(false)
  const [highlight, setHighlight] = useState(-1)
  const [modoNuevo, setModoNuevo] = useState(false)
  const [nuevo, setNuevo] = useState<ProveedorNuevoInput>({ nombre: '' })
  const pickerId = useId()
  const buscarRef = useRef<HTMLInputElement>(null)
  const [reabrir, setReabrir] = useState(false)
  const deferred = useDeferredValue(search.trim().toLowerCase())

  useEffect(() => {
    if (reabrir && buscarRef.current) {
      buscarRef.current.focus()
      setReabrir(false)
    }
  }, [reabrir])

  const query = useQuery({
    queryKey: ['proveedores', { soloActivos: true }],
    queryFn: () => buscarProveedores('', true),
    enabled: open,
    staleTime: 5 * 60_000,
  })

  const todos = query.data ?? []
  const visibles = deferred
    ? todos.filter(
        (proveedor) =>
          proveedor.nombre.toLowerCase().includes(deferred) ||
          proveedor.telefono?.toLowerCase().includes(deferred) ||
          proveedor.documento?.toLowerCase().includes(deferred),
      )
    : todos

  function seleccionar(proveedor: Proveedor) {
    onChange({ proveedorId: proveedor.id, etiqueta: proveedor.nombre })
    setSearch('')
    setOpen(false)
    setHighlight(-1)
  }

  function onKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'Escape') {
      setOpen(false)
      setHighlight(-1)
      return
    }
    if (!open || visibles.length === 0) return
    if (event.key === 'ArrowDown') {
      event.preventDefault()
      setHighlight((i) => (i + 1) % visibles.length)
    } else if (event.key === 'ArrowUp') {
      event.preventDefault()
      setHighlight((i) => (i - 1 + visibles.length) % visibles.length)
    } else if (event.key === 'Enter' && highlight >= 0) {
      event.preventDefault()
      seleccionar(visibles[highlight])
    }
  }

  function abrirNuevo() {
    setNuevo(value.proveedorNuevo ?? { nombre: '' })
    setOpen(false)
    setModoNuevo(true)
  }

  function cambiarProveedor() {
    onChange({})
    setOpen(true)
    setReabrir(true)
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
      <Field label="Proveedor" error={error} hint="Haz clic en el campo para cambiar de proveedor.">
        <span className="picker-selected">
          <input
            value={value.proveedorNuevo ? `${etiqueta} · Nuevo` : etiqueta}
            readOnly
            onClick={cambiarProveedor}
            onKeyDown={(event) => {
              if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault()
                cambiarProveedor()
              }
            }}
            aria-label={`Proveedor seleccionado: ${etiqueta}`}
          />
          <ChevronDown className="picker-selected-caret" size={16} aria-hidden="true" />
        </span>
      </Field>
    )
  }

  return (
    <>
      <div className="picker-root">
        <Field id={pickerId} label="Proveedor" error={error} icon={<Search size={18} aria-hidden="true" />} hint="Haz clic para desplegar la lista o escribe para filtrar por nombre, teléfono o documento.">
          <span className="combobox" data-open={open}>
            <input
              ref={buscarRef}
              id={pickerId}
              role="combobox"
              aria-expanded={open}
              aria-autocomplete="list"
              aria-controls={open ? `${pickerId}-listbox` : undefined}
              value={search}
              onChange={(event) => {
                setSearch(event.target.value)
                setOpen(true)
                setHighlight(-1)
              }}
              onFocus={() => setOpen(true)}
              onBlur={() => setTimeout(() => setOpen(false), 150)}
              onKeyDown={onKeyDown}
              placeholder="Seleccionar proveedor…"
            />
            <ChevronDown className="combobox-caret" size={16} aria-hidden="true" />
          </span>
        </Field>
        {open && (
          <div className="picker-results" id={`${pickerId}-listbox`} role="listbox" aria-label="Proveedores">
            {query.isPending && <div className="picker-empty">Cargando proveedores…</div>}
            {query.isError && <div className="picker-empty">No se pudo cargar la lista de proveedores.</div>}
            {query.data && todos.length === 0 && <div className="picker-empty">No hay proveedores registrados todavía.</div>}
            {query.data && todos.length > 0 && visibles.length === 0 && <div className="picker-empty">Sin resultados para “{search.trim()}”.</div>}
            {query.data && visibles.map((proveedor, i) => (
              <button
                key={proveedor.id}
                type="button"
                role="option"
                aria-selected={i === highlight}
                data-highlight={i === highlight}
                className="picker-option"
                onMouseDown={(event) => {
                  event.preventDefault()
                  seleccionar(proveedor)
                }}
                onMouseEnter={() => setHighlight(i)}
              >
                <span><strong>{proveedor.nombre}</strong>{proveedor.telefono ? ` · ${proveedor.telefono}` : ''}</span>
                <Check size={16} aria-hidden="true" style={{ visibility: i === highlight ? 'visible' : 'hidden' }} />
              </button>
            ))}
          </div>
        )}
      </div>
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