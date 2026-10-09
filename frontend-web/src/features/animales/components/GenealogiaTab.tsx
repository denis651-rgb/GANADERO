import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { Network, Search, Trash2 } from 'lucide-react'
import { crearParentesco, eliminarParentesco, listAnimals, listCategorias, listParentescos, listRazas } from '@/features/animales/api'
import { listPropiedades } from '@/features/propiedades/api'
import { listAllPotreros } from '@/features/potreros/api'
import type { AnimalSummary, Parentesco, TipoParentesco } from '@/features/animales/types'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { ConfirmDialog } from '@/shared/components/ConfirmDialog'
import { EmptyState } from '@/shared/components/EmptyState'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { MobileEntityCard } from '@/shared/components/MobileEntityCard'
import { useToast } from '@/shared/toast/useToast'
import { normalizeApiError } from '@/shared/api/errors'

export function GenealogiaTab({ animalId }: { animalId: string }) {
  const client = useQueryClient()
  const { showToast } = useToast()
  const [showForm, setShowForm] = useState(false)
  const [tipoProgenitor, setTipoProgenitor] = useState<TipoParentesco>('MADRE')
  const [animalPadreId, setAnimalPadreId] = useState('')
  const [busqueda, setBusqueda] = useState('')
  const [menuAbierto, setMenuAbierto] = useState(false)
  const [esExterno, setEsExterno] = useState(false)
  const [removeTarget, setRemoveTarget] = useState<Parentesco | null>(null)

  const query = useQuery({ queryKey: ['animal-parentescos', animalId], queryFn: () => listParentescos(animalId), enabled: Boolean(animalId) })
  const catalogs = useQuery({
    queryKey: ['genealogia-catalogs'],
    queryFn: async () => {
      const [razas, categorias, propiedades, potreros, animales] = await Promise.all([
        listRazas(),
        listCategorias(),
        listPropiedades(),
        listAllPotreros(),
        listAnimals({ search: undefined, estado: '', sexo: '', page: 0, size: 1000 }),
      ])
      return { razas, categorias, propiedades, potreros, animales: animales.content }
    },
  })

  const razasMap = useMemo(() => new Map((catalogs.data?.razas ?? []).map((r) => [r.id, r.nombre])), [catalogs.data?.razas])
  const categoriasMap = useMemo(() => new Map((catalogs.data?.categorias ?? []).map((c) => [c.id, c.nombre])), [catalogs.data?.categorias])
  const propiedadesMap = useMemo(() => new Map((catalogs.data?.propiedades ?? []).map((p) => [p.id, p.nombre])), [catalogs.data?.propiedades])
  const potrerosMap = useMemo(() => new Map((catalogs.data?.potreros ?? []).map((p) => [p.id, p.nombre])), [catalogs.data?.potreros])

  const formatAnimalDesc = (animal: AnimalSummary) => {
    const ident = animal.nombre ? `${animal.nombre} (${animal.codigo})` : animal.codigo
    const detalles: string[] = []
    const raza = razasMap.get(animal.razaPrincipalId)
    if (raza) detalles.push(raza)
    const cat = categoriasMap.get(animal.categoriaActualId)
    if (cat) detalles.push(cat)
    const prop = propiedadesMap.get(animal.propiedadActualId)
    const pot = potrerosMap.get(animal.potreroActualId)
    const ubicacion = [prop, pot].filter(Boolean).join(' / ')
    if (ubicacion) detalles.push(ubicacion)
    if (animal.estado && animal.estado !== 'ACTIVO') detalles.push(animal.estado)
    return detalles.length > 0 ? `${ident} · [${detalles.join(' · ')}]` : ident
  }

  const sexoEsperado = tipoProgenitor === 'MADRE' ? 'HEMBRA' : 'MACHO'
  const animalesElegibles = useMemo(() => {
    return (catalogs.data?.animales ?? []).filter((a) => a.id !== animalId && a.sexo === sexoEsperado)
  }, [catalogs.data?.animales, animalId, sexoEsperado])

  const animalesFiltrados = useMemo(() => {
    const q = busqueda.trim().toLowerCase()
    if (!q) return animalesElegibles
    return animalesElegibles.filter((a) => {
      const codigoMatch = a.codigo.toLowerCase().includes(q)
      const nombreMatch = (a.nombre ?? '').toLowerCase().includes(q)
      const razaMatch = (razasMap.get(a.razaPrincipalId) ?? '').toLowerCase().includes(q)
      const catMatch = (categoriasMap.get(a.categoriaActualId) ?? '').toLowerCase().includes(q)
      const propMatch = (propiedadesMap.get(a.propiedadActualId) ?? '').toLowerCase().includes(q)
      const potMatch = (potrerosMap.get(a.potreroActualId) ?? '').toLowerCase().includes(q)
      return codigoMatch || nombreMatch || razaMatch || catMatch || propMatch || potMatch
    })
  }, [animalesElegibles, busqueda, razasMap, categoriasMap, propiedadesMap, potrerosMap])

  const selectedAnimal = useMemo(() => {
    if (!animalPadreId) return null
    return (catalogs.data?.animales ?? []).find((a) => a.id === animalPadreId) ?? null
  }, [catalogs.data?.animales, animalPadreId])

  const create = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      return crearParentesco(animalId, {
        tipo: tipoProgenitor,
        animalPadreId: esExterno ? undefined : (animalPadreId || undefined),
        nombreExterno: esExterno ? (String(data.get('nombreExterno')) || undefined) : undefined,
        razaExternaId: esExterno ? (String(data.get('razaExternaId')) || undefined) : undefined,
        registroGenealogico: esExterno ? (String(data.get('registroGenealogico')) || undefined) : undefined,
      })
    },
    onSuccess: async () => {
      setShowForm(false)
      setAnimalPadreId('')
      setBusqueda('')
      showToast('Parentesco registrado correctamente.')
      await client.invalidateQueries({ queryKey: ['animal-parentescos', animalId] })
    },
  })

  const remove = useMutation({
    mutationFn: (parentescoId: string) => eliminarParentesco(animalId, parentescoId),
    onSuccess: async () => {
      setRemoveTarget(null)
      await client.invalidateQueries({ queryKey: ['animal-parentescos', animalId] })
    },
  })

  const error = query.error ?? catalogs.error ?? create.error ?? remove.error

  return <div className="page-stack">
    {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
    <Card>
      <div className="filter-heading">
        <span><Network size={18} />Genealogía</span>
        <Button variant="secondary" onClick={() => { setShowForm((value) => !value); setAnimalPadreId(''); setBusqueda('') }}>
          {showForm ? 'Cancelar' : 'Registrar progenitor'}
        </Button>
      </div>

      {showForm && <form className="form-grid compact-form" onSubmit={(event) => { event.preventDefault(); create.mutate(event.currentTarget) }}>
        <Field label="Tipo de progenitor">
          <select
            name="tipo"
            required
            value={tipoProgenitor}
            onChange={(event) => {
              const nuevoTipo = event.target.value as TipoParentesco
              setTipoProgenitor(nuevoTipo)
              setAnimalPadreId('')
            }}
          >
            <option value="MADRE">Madre (Hembra)</option>
            <option value="PADRE">Padre (Macho)</option>
          </select>
        </Field>

        <label className="checkbox-line">
          <input type="checkbox" checked={esExterno} onChange={(event) => setEsExterno(event.target.checked)} />
          Progenitor externo (sin ficha en el sistema)
        </label>

        {esExterno ? <>
          <Field label="Nombre externo"><input name="nombreExterno" maxLength={160} placeholder="Ej. Toro de la Hacienda San Roque" /></Field>
          <Field label="Raza"><select name="razaExternaId"><option value="">Sin especificar</option>{catalogs.data?.razas.map((raza) => <option key={raza.id} value={raza.id}>{raza.nombre}</option>)}</select></Field>
          <Field label="Registro genealógico"><input name="registroGenealogico" maxLength={160} /></Field>
        </> : <>
          <input type="hidden" name="animalPadreId" value={animalPadreId} />
          <div className="picker-root form-full">
            <Field
              id="buscar-progenitor-input"
              label={`Animal progenitor (${tipoProgenitor === 'MADRE' ? 'Hembra' : 'Macho'})`}
              icon={<Search size={18} aria-hidden="true" />}
              hint={selectedAnimal ? 'Haz clic en el campo o en Cambiar para buscar otro animal.' : `Escribe para filtrar en vivo entre las ${animalesElegibles.length} ${tipoProgenitor === 'MADRE' ? 'hembras' : 'machos'} disponibles.`}
            >
              {selectedAnimal ? (
                <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', width: '100%' }}>
                  <input
                    id="buscar-progenitor-input"
                    readOnly
                    value={formatAnimalDesc(selectedAnimal)}
                    style={{ flex: 1, fontWeight: 600 }}
                  />
                  <Button
                    type="button"
                    variant="secondary"
                    onClick={() => {
                      setAnimalPadreId('')
                      setBusqueda('')
                      setMenuAbierto(true)
                    }}
                  >
                    Cambiar
                  </Button>
                </div>
              ) : (
                <span className="combobox" data-open={menuAbierto}>
                  <input
                    id="buscar-progenitor-input"
                    role="combobox"
                    aria-expanded={menuAbierto}
                    aria-autocomplete="list"
                    autoComplete="off"
                    value={busqueda}
                    onChange={(event) => {
                      setBusqueda(event.target.value)
                      setMenuAbierto(true)
                    }}
                    onFocus={() => setMenuAbierto(true)}
                    onBlur={() => setTimeout(() => setMenuAbierto(false), 200)}
                    placeholder={`Escribe para buscar ${tipoProgenitor === 'MADRE' ? 'hembra' : 'macho'} por nombre, arete/código, raza o potrero…`}
                  />
                </span>
              )}
            </Field>

            {menuAbierto && !selectedAnimal && (
              <div className="picker-results" role="listbox" aria-label={`Lista de ${tipoProgenitor === 'MADRE' ? 'hembras' : 'machos'}`}>
                {animalesFiltrados.length === 0 ? (
                  <div className="picker-empty">
                    No se encontró ningún animal de sexo {tipoProgenitor === 'MADRE' ? 'hembra' : 'macho'} que coincida con “{busqueda.trim()}”.
                  </div>
                ) : (
                  animalesFiltrados.slice(0, 30).map((animal) => {
                    const ident = animal.nombre ? `${animal.nombre} (${animal.codigo})` : animal.codigo
                    const detalles: string[] = []
                    const raza = razasMap.get(animal.razaPrincipalId)
                    if (raza) detalles.push(raza)
                    const cat = categoriasMap.get(animal.categoriaActualId)
                    if (cat) detalles.push(cat)
                    const prop = propiedadesMap.get(animal.propiedadActualId)
                    const pot = potrerosMap.get(animal.potreroActualId)
                    const ubicacion = [prop, pot].filter(Boolean).join(' / ')
                    if (ubicacion) detalles.push(ubicacion)

                    return (
                      <button
                        key={animal.id}
                        type="button"
                        className="picker-option"
                        onMouseDown={(event) => {
                          event.preventDefault()
                          setAnimalPadreId(animal.id)
                          setBusqueda('')
                          setMenuAbierto(false)
                        }}
                        style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: '0.2rem', padding: '0.5rem 0.75rem', width: '100%', textAlign: 'left' }}
                      >
                        <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{ident}</span>
                        {detalles.length > 0 && (
                          <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                            {detalles.join(' · ')}
                          </span>
                        )}
                      </button>
                    )
                  })
                )}
              </div>
            )}
          </div>
        </>}
        <div className="form-actions"><Button type="submit" loading={create.isPending}>Guardar parentesco</Button></div>
      </form>}

      {query.isPending && <LoadingState message="Cargando genealogía…" />}
      {query.data?.length === 0 && !showForm && <EmptyState title="Sin progenitores" description="Registra la madre o el padre del animal." />}
      {query.data && query.data.length > 0 && <><div className="table-wrapper desktop-only"><table><caption className="visually-hidden">Relaciones genealógicas del animal</caption><thead><tr><th scope="col">Rol</th><th scope="col">Progenitor</th><th scope="col">Registro genealógico</th><th scope="col">Fecha</th><th scope="col">Acciones</th></tr></thead><tbody>{query.data.map((item) => <tr key={item.id}>
        <td><span className="status-badge">{item.tipo}</span></td>
        <td>{item.animalPadreId ? (() => {
          const padre = catalogs.data?.animales.find((animal) => animal.id === item.animalPadreId);
          if (!padre) return <strong>Animal registrado</strong>
          const extra = [razasMap.get(padre.razaPrincipalId), categoriasMap.get(padre.categoriaActualId)].filter(Boolean).join(' · ')
          return <>
            <strong>{padre.nombre ? `${padre.nombre} (${padre.codigo})` : padre.codigo}</strong>
            {extra ? <span className="table-secondary" style={{ marginLeft: '0.4rem' }}>[{extra}]</span> : null}
          </>
        })() : <strong>{item.nombreExterno ?? 'Progenitor externo'}</strong>}{item.razaExternaId ? ` · ${razasMap.get(item.razaExternaId) ?? 'Raza'}` : ''}</td>
        <td>{item.registroGenealogico ?? '—'}</td>
        <td>{new Date(item.fechaRegistro).toLocaleDateString('es-BO')}</td>
        <td><Button variant="danger" loading={remove.isPending && remove.variables === item.id} onClick={() => setRemoveTarget(item)}><Trash2 size={16} />Eliminar</Button></td>
      </tr>)}</tbody></table></div><div className="mobile-only"><div className="mobile-entity-list">{query.data.map((item) => {
        const padre = item.animalPadreId ? catalogs.data?.animales.find((animal) => animal.id === item.animalPadreId) : undefined
        const progenitor = item.animalPadreId ? (padre ? `${padre.nombre ? `${padre.nombre} (${padre.codigo})` : padre.codigo}` : 'Animal registrado') : (item.nombreExterno ?? 'Progenitor externo')
        const raza = item.razaExternaId ? razasMap.get(item.razaExternaId) : (padre ? razasMap.get(padre.razaPrincipalId) : undefined)
        return <MobileEntityCard key={item.id} title={progenitor} status={<span className="status-badge">{item.tipo}</span>} subtitle={raza} metadata={<><span>Registro: {item.registroGenealogico ?? 'Sin registro'}</span><span>Registrado: {new Date(item.fechaRegistro).toLocaleDateString('es-BO')}</span></>} action={<Button variant="danger" loading={remove.isPending && remove.variables === item.id} onClick={() => setRemoveTarget(item)}><Trash2 size={16} aria-hidden="true" />Eliminar</Button>} />
      })}</div></div></>}
    </Card>

    <ConfirmDialog
      open={Boolean(removeTarget)}
      title="Eliminar parentesco"
      confirmLabel="Eliminar parentesco"
      confirmIcon={<Trash2 size={16} />}
      loading={remove.isPending}
      error={remove.error}
      onClose={() => setRemoveTarget(null)}
      onConfirm={() => { if (removeTarget) remove.mutate(removeTarget.id) }}
    >
      {removeTarget && (
        <>
          <p className="muted">
            ¿Eliminar el parentesco de <strong>{removeTarget.tipo === 'MADRE' ? 'madre' : 'padre'}</strong>:{' '}
            <strong>{removeTarget.animalPadreId
              ? (() => { const padre = catalogs.data?.animales.find((animal) => animal.id === removeTarget.animalPadreId); return padre ? `${padre.nombre ? `${padre.nombre} (${padre.codigo})` : padre.codigo}` : 'Animal registrado' })()
              : (removeTarget.nombreExterno ?? 'Progenitor externo')}</strong>?
          </p>
          <p className="muted">Esta acción no se puede deshacer.</p>
        </>
      )}
    </ConfirmDialog>
  </div>
}

