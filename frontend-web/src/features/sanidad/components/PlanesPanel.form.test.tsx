import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { PlanesPanel } from './PlanesPanel'
import {
  actualizarPlanItem, crearPlanItem, listConflictosActivacion, listPlanItems, type PlanSanitario, type PlanSanitarioItem,
} from '@/features/sanidad/api'
import { AppError } from '@/shared/api/errors'
import type { SanidadCatalogs } from '@/features/sanidad/catalogs'

vi.mock('@/auth/auth-context', () => ({ useAuth: () => ({ can: () => true, user: { id: 'u-1' } }) }))
vi.mock('@/features/sanidad/api', async () => {
  const actual = await vi.importActual<typeof import('@/features/sanidad/api')>('@/features/sanidad/api')
  return {
    ...actual,
    listPlanItems: vi.fn().mockResolvedValue([]),
    crearPlanItem: vi.fn().mockResolvedValue({}),
    actualizarPlanItem: vi.fn().mockResolvedValue({}),
    listConflictosActivacion: vi.fn().mockResolvedValue([]),
  }
})

const plan = {
  id: 'plan-1', nombre: 'Plan 2026', descripcion: null, estado: 'ACTIVO', version: 0,
  fechaInicio: '2026-01-01', fechaFin: null,
} as unknown as PlanSanitario

const catalogs = {
  properties: [], paddocks: [], lots: [], animals: [], animalLabel: () => '',
  categories: [
    { id: 'c-vaca', codigo: 'VACA', nombre: 'Vaca', sexoAplicable: 'HEMBRA' },
    { id: 'c-vaq', codigo: 'VAQ', nombre: 'Vaquillona', sexoAplicable: 'HEMBRA' },
    { id: 'c-toro', codigo: 'TORO', nombre: 'Toro', sexoAplicable: 'MACHO' },
  ],
} as SanidadCatalogs

async function abrirFormulario() {
  const user = userEvent.setup()
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(<QueryClientProvider client={client}><PlanesPanel planes={[plan]} isLoading={false} error={null} catalogs={catalogs} refresh={() => {}} /></QueryClientProvider>)
  await user.click(screen.getByRole('button', { name: /Agregar actividad/ }))
  const dialog = await screen.findByRole('dialog')
  return { user, dialog }
}

function payloadEnviado() {
  return vi.mocked(crearPlanItem).mock.calls[0][1]
}

describe('formulario de actividad del plan sanitario', () => {
  beforeEach(() => {
    vi.mocked(crearPlanItem).mockClear()
    vi.mocked(listPlanItems).mockResolvedValue([])
    vi.mocked(actualizarPlanItem).mockReset().mockResolvedValue({} as PlanSanitarioItem)
    vi.mocked(listConflictosActivacion).mockResolvedValue([])
  })

  it('ordena las secciones de lo general a lo específico', async () => {
    const { dialog } = await abrirFormulario()
    const secciones = within(dialog).getAllByRole('heading', { level: 4 }).map((h) => h.textContent)
    expect(secciones).toEqual([
      'Datos generales', 'Cuándo se programa', 'A quién aplica',
      'Medicamento recomendado (informativo, no es inventario)', 'Dosis', 'Vía y lugar de aplicación', 'Alertas',
    ])
  })

  it('permite elegir varias categorías y las envía todas', async () => {
    const { user, dialog } = await abrirFormulario()
    expect(within(dialog).getByText('Sin selección: aplica a todas las categorías.')).toBeInTheDocument()

    await user.type(within(dialog).getByLabelText(/Nombre de la actividad/), 'Aftosa')
    await user.selectOptions(within(dialog).getByLabelText(/Clasificación regulatoria/), 'RECOMENDADO_VETERINARIO')
    await user.click(within(dialog).getByLabelText('Vaca'))
    await user.click(within(dialog).getByLabelText('Vaquillona'))
    await user.click(within(dialog).getByLabelText('Toro'))
    await user.click(within(dialog).getByLabelText('Toro'))
    expect(within(dialog).getByText('2 seleccionada(s).')).toBeInTheDocument()

    await user.click(within(dialog).getByRole('button', { name: 'Agregar actividad' }))
    expect(payloadEnviado().categoriasAplicables).toEqual(['c-vaca', 'c-vaq'])
  })

  it('llama «Recomendado por veterinario» a esa clasificación', async () => {
    const { dialog } = await abrirFormulario()
    expect(within(dialog).getByRole('option', { name: 'Recomendado por veterinario' })).toBeInTheDocument()
  })

  it('marca y bloquea «obligatoria» cuando SENASAG la exige, y la deja libre en otros casos', async () => {
    const { user, dialog } = await abrirFormulario()
    const obligatoria = within(dialog).getByLabelText('Actividad obligatoria')
    expect(obligatoria).not.toBeChecked()
    expect(obligatoria).toBeEnabled()

    await user.selectOptions(within(dialog).getByLabelText(/Clasificación regulatoria/), 'OBLIGATORIO_SENASAG')
    expect(obligatoria).toBeChecked()
    expect(obligatoria).toBeDisabled()

    await user.type(within(dialog).getByLabelText(/Nombre de la actividad/), 'Aftosa')
    await user.click(within(dialog).getByRole('button', { name: 'Agregar actividad' }))
    expect(payloadEnviado()).toMatchObject({ origenRegulatorio: 'OBLIGATORIO_SENASAG', obligatorio: true })
  })

  it('pide la edad objetivo con unidad y la envía tal cual, sin convertirla a días', async () => {
    const { user, dialog } = await abrirFormulario()
    await user.type(within(dialog).getByLabelText(/Nombre de la actividad/), 'Desparasitación al destete')
    await user.selectOptions(within(dialog).getByLabelText(/Clasificación regulatoria/), 'RECOMENDADO_VETERINARIO')
    await user.selectOptions(within(dialog).getByLabelText(/^Modalidad/), 'POR_EDAD')

    await user.type(within(dialog).getByLabelText(/Edad objetivo/), '7')
    expect(within(dialog).getByLabelText('Unidad de la edad objetivo')).toHaveValue('MESES')

    await user.click(within(dialog).getByRole('button', { name: 'Agregar actividad' }))
    expect(payloadEnviado().modalidadConfig).toMatchObject({ edadObjetivoValor: 7, edadUnidad: 'MESES', unaVezEnLaVida: true })
  })

  it('ofrece «una sola vez en la vida» marcada por defecto y permite desmarcarla', async () => {
    const { user, dialog } = await abrirFormulario()
    await user.type(within(dialog).getByLabelText(/Nombre de la actividad/), 'Refuerzo')
    await user.selectOptions(within(dialog).getByLabelText(/Clasificación regulatoria/), 'RECOMENDADO_VETERINARIO')
    await user.selectOptions(within(dialog).getByLabelText(/^Modalidad/), 'POR_EDAD')
    await user.type(within(dialog).getByLabelText(/Edad objetivo/), '3')

    const unaVez = within(dialog).getByLabelText('Una sola vez en la vida del animal')
    expect(unaVez).toBeChecked()
    expect(within(dialog).getByText(/no se vuelve a programar/)).toBeInTheDocument()

    await user.click(unaVez)
    expect(unaVez).not.toBeChecked()
    expect(within(dialog).getByText(/Se programa aunque el animal ya la haya recibido/)).toBeInTheDocument()

    await user.click(within(dialog).getByRole('button', { name: 'Agregar actividad' }))
    expect(payloadEnviado().modalidadConfig).toMatchObject({ unaVezEnLaVida: false })
  })

  it('avisa y bloquea el guardado si la edad objetivo queda fuera del rango de elegibles', async () => {
    const { user, dialog } = await abrirFormulario()
    await user.type(within(dialog).getByLabelText(/Nombre de la actividad/), 'Desparasitación al destete')
    await user.selectOptions(within(dialog).getByLabelText(/Clasificación regulatoria/), 'RECOMENDADO_VETERINARIO')
    await user.selectOptions(within(dialog).getByLabelText(/^Modalidad/), 'POR_EDAD')
    await user.type(within(dialog).getByLabelText(/Edad objetivo/), '7') // 7 meses = 210 días
    const guardar = within(dialog).getByRole('button', { name: 'Agregar actividad' })
    expect(guardar).toBeEnabled()

    await user.type(within(dialog).getByLabelText(/^Hasta/), '6') // elegibles hasta 6 meses = 180 días
    expect(within(dialog).getAllByText(/La edad objetivo \(210 días\) supera la edad máxima/)).not.toHaveLength(0)
    expect(within(dialog).getByLabelText(/Edad objetivo/)).toBeInvalid()
    expect(guardar).toBeDisabled()

    await user.clear(within(dialog).getByLabelText(/^Hasta/))
    await user.type(within(dialog).getByLabelText(/^Hasta/), '8')
    expect(within(dialog).queryByText(/supera la edad máxima/)).not.toBeInTheDocument()
    expect(guardar).toBeEnabled()

    await user.click(guardar)
    expect(payloadEnviado().modalidadConfig).toMatchObject({ edadObjetivoValor: 7, edadUnidad: 'MESES' })
    expect(payloadEnviado()).toMatchObject({ edadMaxDias: 240 })
  })

  it('no valida la edad objetivo en modalidades que no son por edad', async () => {
    const { user, dialog } = await abrirFormulario()
    await user.selectOptions(within(dialog).getByLabelText(/^Modalidad/), 'PERIODICA')
    await user.type(within(dialog).getByLabelText(/^Hasta/), '6')
    expect(within(dialog).getByRole('button', { name: 'Agregar actividad' })).toBeEnabled()
  })

  it('no ofrece «una sola vez» en modalidades que no son por edad', async () => {
    const { user, dialog } = await abrirFormulario()
    await user.selectOptions(within(dialog).getByLabelText(/^Modalidad/), 'PERIODICA')
    expect(within(dialog).queryByLabelText('Una sola vez en la vida del animal')).not.toBeInTheDocument()
  })

  it('al editar, carga lo guardado en la actividad (7 meses, no una sola vez)', async () => {
    vi.mocked(listPlanItems).mockResolvedValue([{
      id: 'item-1', nombre: 'Desparasitación al destete', tipoActividad: 'DESPARASITACION', modalidad: 'POR_EDAD',
      origenRegulatorio: 'RECOMENDADO_VETERINARIO', obligatorio: false, dosisTipoCalculo: 'NO_APLICA', activo: true,
      version: 0, numeroVersion: 1, categoriasAplicables: [], diasAlerta: 0, horariosAviso: ['08:00'], horaEjecucion: '08:00',
      modalidadConfig: {
        edadObjetivoValor: 7, edadUnidad: 'MESES', ventanaAnticipadaDias: 15, ventanaPosteriorDias: 30,
        politicaEdadEstimada: 'EXCLUIR', politicaEdadDesconocida: 'EXCLUIR', unaVezEnLaVida: false,
      },
    } as unknown as PlanSanitarioItem])
    const user = userEvent.setup()
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(<QueryClientProvider client={client}><PlanesPanel planes={[plan]} isLoading={false} error={null} catalogs={catalogs} refresh={() => {}} /></QueryClientProvider>)

    await user.click(screen.getByRole('button', { name: /Mostrar actividades del plan Plan 2026/ }))
    await user.click(await screen.findByRole('button', { name: 'Editar Desparasitación al destete' }))
    const dialog = await screen.findByRole('dialog')

    expect(within(dialog).getByLabelText('Una sola vez en la vida del animal')).not.toBeChecked()
    expect(within(dialog).getByLabelText(/Edad objetivo/)).toHaveValue(7)
    expect(within(dialog).getByLabelText('Unidad de la edad objetivo')).toHaveValue('MESES')
    expect(within(dialog).getByLabelText(/Ventana posterior/)).toHaveValue(30)
    expect(within(dialog).getByLabelText(/Excluir animales con fecha de nacimiento estimada/)).toBeChecked()
  })

  it('fija el peso de referencia según la unidad «ml por 50 kg»', async () => {
    const { user, dialog } = await abrirFormulario()
    await user.type(within(dialog).getByLabelText(/Nombre de la actividad/), 'Ivermectina')
    await user.selectOptions(within(dialog).getByLabelText(/Clasificación regulatoria/), 'RECOMENDADO_VETERINARIO')
    await user.selectOptions(within(dialog).getByLabelText(/Tipo de cálculo/), 'POR_PESO')
    await user.type(within(dialog).getByLabelText(/^Cantidad/), '1')

    const peso = within(dialog).getByLabelText(/Peso de referencia/)
    expect(peso).toBeEnabled()
    expect(peso).not.toHaveAttribute('readonly')

    await user.selectOptions(within(dialog).getByLabelText('Unidad'), 'ML_POR_50KG')
    expect(peso).toHaveValue(50)
    expect(peso).toHaveAttribute('readonly')

    await user.click(within(dialog).getByRole('button', { name: 'Agregar actividad' }))
    expect(payloadEnviado()).toMatchObject({ dosisTipoCalculo: 'POR_PESO', dosisUnidad: 'ML_POR_50KG', dosisPesoReferenciaKg: 50 })
  })

  it('deja escribir el peso de referencia con una unidad absoluta', async () => {
    const { user, dialog } = await abrirFormulario()
    await user.selectOptions(within(dialog).getByLabelText(/Tipo de cálculo/), 'POR_PESO')
    await user.selectOptions(within(dialog).getByLabelText('Unidad'), 'ML')
    const peso = within(dialog).getByLabelText(/Peso de referencia/)
    expect(peso).not.toHaveAttribute('readonly')
    await user.type(peso, '25')
    expect(peso).toHaveValue(25)
  })

  // ---------- planes: fechas y estados ----------

  function renderPanel(planes: PlanSanitario[]) {
    const user = userEvent.setup()
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(<QueryClientProvider client={client}><PlanesPanel planes={planes} isLoading={false} error={null} catalogs={catalogs} refresh={() => {}} /></QueryClientProvider>)
    return user
  }
  const planEn = (id: string, estado: string) => ({ ...plan, id, nombre: `Plan ${estado}`, estado }) as unknown as PlanSanitario

  it('no deja crear un plan con la fecha de fin anterior a la de inicio', async () => {
    const user = renderPanel([plan])
    await user.click(screen.getByRole('button', { name: /Nuevo plan/ }))
    const dialog = await screen.findByRole('dialog')
    fireEvent.change(within(dialog).getByLabelText(/Fecha de inicio/), { target: { value: '2026-06-01' } })
    fireEvent.change(within(dialog).getByLabelText(/Fecha de fin/), { target: { value: '2026-05-31' } })

    expect(within(dialog).getByText('La fecha de fin no puede ser anterior a la fecha de inicio.')).toBeInTheDocument()
    expect(within(dialog).getByRole('button', { name: 'Crear plan' })).toBeDisabled()

    fireEvent.change(within(dialog).getByLabelText(/Fecha de fin/), { target: { value: '2026-06-30' } })
    expect(within(dialog).getByRole('button', { name: 'Crear plan' })).toBeEnabled()
  })

  it('permite agregar actividades a planes en borrador y activos, no a los cerrados', () => {
    renderPanel([planEn('p1', 'BORRADOR'), planEn('p2', 'ACTIVO'), planEn('p3', 'FINALIZADO'), planEn('p4', 'ANULADO')])
    expect(screen.getAllByRole('button', { name: /Agregar actividad/ })).toHaveLength(2)
  })

  it('en un plan cerrado no muestra acciones sobre sus actividades y lo explica', async () => {
    vi.mocked(listPlanItems).mockResolvedValue([itemManual()])
    const user = renderPanel([planEn('p3', 'FINALIZADO')])
    await user.click(screen.getByRole('button', { name: /Mostrar actividades del plan/ }))

    expect(await screen.findByText(/Este plan está finalizado: sus actividades ya no se pueden modificar/)).toBeInTheDocument()
    expect(await screen.findByText('Actividad de prueba')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Editar/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Desactivar/ })).not.toBeInTheDocument()
  })

  it('al activar un plan avisa, sin bloquear, si ya hay otro activo con actividades del mismo tipo', async () => {
    vi.mocked(listConflictosActivacion).mockResolvedValue([planEn('p9', 'ACTIVO')])
    const user = renderPanel([planEn('p1', 'BORRADOR')])
    await user.click(screen.getByRole('button', { name: 'Activar' }))

    expect(await screen.findByText(/podrían solaparse/)).toBeInTheDocument()
    expect(screen.getByText('Plan ACTIVO')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Confirmar' })).toBeEnabled()
  })

  it('al activar un plan sin solapamientos no muestra ningún aviso', async () => {
    const user = renderPanel([planEn('p1', 'BORRADOR')])
    await user.click(screen.getByRole('button', { name: 'Activar' }))
    await screen.findByText(/pasará al estado/)
    await vi.waitFor(() => expect(listConflictosActivacion).toHaveBeenCalledWith('p1'))
    expect(screen.queryByText(/podrían solaparse/)).not.toBeInTheDocument()
  })

  it('al finalizar un plan avisa que se cancelan los pendientes', async () => {
    const user = renderPanel([planEn('p2', 'ACTIVO')])
    await user.click(screen.getByRole('button', { name: 'Finalizar' }))
    expect(await screen.findByText(/Se cancelarán los eventos pendientes de sus actividades/)).toBeInTheDocument()
  })

  it('al desactivar una actividad avisa que se cancelan sus pendientes, y al reactivarla que se restauran', async () => {
    vi.mocked(listPlanItems).mockResolvedValue([itemManual()])
    const user = renderPanel([planEn('p2', 'ACTIVO')])
    await user.click(screen.getByRole('button', { name: /Mostrar actividades del plan/ }))
    await user.click(await screen.findByRole('button', { name: /^Desactivar Actividad de prueba/ }))
    expect(await screen.findByText('Se cancelarán sus eventos pendientes del calendario.')).toBeInTheDocument()
  })

  // ---------- actividad: validaciones antes de enviar ----------

  it('una vía inyectable sin lugar anatómico se avisa y bloquea el guardado', async () => {
    const { user, dialog } = await abrirFormulario()
    const guardar = within(dialog).getByRole('button', { name: 'Agregar actividad' })
    await user.selectOptions(within(dialog).getByLabelText('Vía de administración'), 'SUBCUTANEA')

    expect(within(dialog).getByText('Una vía inyectable requiere indicar el lugar anatómico.')).toBeInTheDocument()
    expect(guardar).toBeDisabled()

    await user.selectOptions(within(dialog).getByLabelText('Lugar anatómico'), 'TABLA_DEL_CUELLO')
    expect(within(dialog).queryByText(/requiere indicar el lugar anatómico/)).not.toBeInTheDocument()
    expect(guardar).toBeEnabled()
  })

  it('«otra» vía exige su detalle antes de poder guardar', async () => {
    const { user, dialog } = await abrirFormulario()
    const guardar = within(dialog).getByRole('button', { name: 'Agregar actividad' })
    await user.selectOptions(within(dialog).getByLabelText('Vía de administración'), 'OTRA')
    expect(within(dialog).getByText('Indica el detalle de la vía.')).toBeInTheDocument()
    expect(guardar).toBeDisabled()

    await user.type(within(dialog).getByLabelText(/Detalle de la vía/), 'Intramamaria')
    expect(guardar).toBeEnabled()
  })

  it('la dosis por peso exige cantidad, y la máxima no puede ser menor que la mínima', async () => {
    const { user, dialog } = await abrirFormulario()
    const guardar = within(dialog).getByRole('button', { name: 'Agregar actividad' })
    await user.selectOptions(within(dialog).getByLabelText(/Tipo de cálculo/), 'POR_PESO')
    await user.selectOptions(within(dialog).getByLabelText('Unidad'), 'ML_POR_50KG')
    expect(within(dialog).getByText('La dosis por peso requiere una cantidad.')).toBeInTheDocument()
    expect(guardar).toBeDisabled()

    await user.type(within(dialog).getByLabelText(/^Cantidad/), '1')
    expect(guardar).toBeEnabled()

    await user.type(within(dialog).getByLabelText(/^Dosis mínima/), '5')
    await user.type(within(dialog).getByLabelText(/^Dosis máxima/), '2')
    expect(within(dialog).getByText('La dosis máxima no puede ser menor que la mínima.')).toBeInTheDocument()
    expect(guardar).toBeDisabled()

    await user.clear(within(dialog).getByLabelText(/^Dosis máxima/))
    await user.type(within(dialog).getByLabelText(/^Dosis máxima/), '10')
    expect(guardar).toBeEnabled()
  })

  it('la modalidad por hallazgo exige elegir al menos un hallazgo', async () => {
    const { user, dialog } = await abrirFormulario()
    const guardar = within(dialog).getByRole('button', { name: 'Agregar actividad' })
    await user.selectOptions(within(dialog).getByLabelText(/^Modalidad/), 'POR_HALLAZGO')
    expect(within(dialog).getByText('Elige al menos un hallazgo que active la actividad.')).toBeInTheDocument()
    expect(guardar).toBeDisabled()

    await user.click(within(dialog).getByLabelText('Caso clínico abierto'))
    expect(guardar).toBeEnabled()
  })

  it('la fecha programada se guarda como una sola vez y ya no ofrece una casilla que no hacía nada', async () => {
    const { user, dialog } = await abrirFormulario()
    await user.type(within(dialog).getByLabelText(/Nombre de la actividad/), 'Control puntual')
    await user.selectOptions(within(dialog).getByLabelText(/Clasificación regulatoria/), 'RECOMENDADO_VETERINARIO')
    await user.selectOptions(within(dialog).getByLabelText(/^Modalidad/), 'FECHA_PROGRAMADA')

    expect(within(dialog).queryByLabelText('Una sola vez')).not.toBeInTheDocument()
    expect(within(dialog).getByText(/Se programa una sola vez, en esa fecha/)).toBeInTheDocument()

    fireEvent.change(within(dialog).getByLabelText(/Fecha y hora programada/), { target: { value: '2026-07-01T09:00' } })
    await user.click(within(dialog).getByRole('button', { name: 'Agregar actividad' }))
    expect(payloadEnviado().modalidadConfig).toMatchObject({ unicaVez: true })
  })

  // ---------- editar: se carga lo guardado de cada modalidad ----------

  function itemManual(extra: Record<string, unknown> = {}) {
    return {
      id: 'item-1', nombre: 'Actividad de prueba', tipoActividad: 'DESPARASITACION', modalidad: 'MANUAL', modalidadConfig: {},
      origenRegulatorio: 'RECOMENDADO_VETERINARIO', obligatorio: false, dosisTipoCalculo: 'NO_APLICA', activo: true,
      version: 0, numeroVersion: 1, categoriasAplicables: [], diasAlerta: 0, horariosAviso: ['08:00'], horaEjecucion: '08:00',
      ...extra,
    } as unknown as PlanSanitarioItem
  }

  async function editar(item: PlanSanitarioItem) {
    vi.mocked(listPlanItems).mockResolvedValue([item])
    const user = renderPanel([plan])
    await user.click(screen.getByRole('button', { name: /Mostrar actividades del plan/ }))
    await user.click(await screen.findByRole('button', { name: `Editar ${item.nombre}` }))
    return { user, dialog: within(await screen.findByRole('dialog')) }
  }

  it('al editar una actividad periódica carga su frecuencia, referencia y tolerancias', async () => {
    const { dialog } = await editar(itemManual({
      modalidad: 'PERIODICA',
      modalidadConfig: { frecuenciaValor: 4, frecuenciaUnidad: 'MESES', referenciaCalculo: 'FECHA_DE_NACIMIENTO', toleranciaAnticipadaDias: 3, toleranciaPosteriorDias: 9 },
    }))
    expect(dialog.getByLabelText(/^Frecuencia/)).toHaveValue(4)
    expect(dialog.getByLabelText('Unidad de frecuencia')).toHaveValue('MESES')
    expect(dialog.getByLabelText('Se calcula desde')).toHaveValue('FECHA_DE_NACIMIENTO')
    expect(dialog.getByLabelText(/Tolerancia anticipada/)).toHaveValue(3)
    expect(dialog.getByLabelText(/Tolerancia posterior/)).toHaveValue(9)
  })

  it('al editar una actividad de fecha programada carga la fecha y hora guardadas', async () => {
    const guardada = new Date(2026, 6, 1, 9, 30)
    const { dialog } = await editar(itemManual({
      modalidad: 'FECHA_PROGRAMADA',
      modalidadConfig: { fechaProgramada: guardada.toISOString(), unicaVez: true },
    }))
    expect(dialog.getByLabelText(/Fecha y hora programada/)).toHaveValue('2026-07-01T09:30')
  })

  it('al editar una actividad por hallazgo carga los hallazgos, el plazo y la validación veterinaria', async () => {
    const { dialog } = await editar(itemManual({
      modalidad: 'POR_HALLAZGO',
      modalidadConfig: { tiposHallazgo: ['CASO_CLINICO_ABIERTO'], plazoDias: 5, requiereValidacionVeterinaria: true },
    }))
    expect(dialog.getByLabelText('Caso clínico abierto')).toBeChecked()
    expect(dialog.getByLabelText('Examen reproductivo no apto')).not.toBeChecked()
    expect(dialog.getByLabelText(/Plazo para resolverlo/)).toHaveValue(5)
    expect(dialog.getByLabelText(/Requiere validación veterinaria/)).toBeChecked()
  })

  it('al editar carga la dosis, la vía y sus detalles guardados', async () => {
    const { dialog } = await editar(itemManual({
      dosisTipoCalculo: 'POR_PESO', dosisCantidad: 1, dosisUnidad: 'ML', dosisPesoReferenciaKg: 50, dosisMinima: 2, dosisMaxima: 10,
      viaAdministracionCodigo: 'OTRA', viaAdministracionDetalle: 'Intramamaria',
      lugarAplicacion: 'OTRO', lugarAplicacionDetalle: 'Pezuña',
    }))
    expect(dialog.getByLabelText(/^Cantidad/)).toHaveValue(1)
    expect(dialog.getByLabelText(/^Dosis mínima/)).toHaveValue(2)
    expect(dialog.getByLabelText(/^Dosis máxima/)).toHaveValue(10)
    expect(dialog.getByLabelText(/Detalle de la vía/)).toHaveValue('Intramamaria')
    expect(dialog.getByLabelText(/Detalle del lugar/)).toHaveValue('Pezuña')
  })

  // ---------- editar una actividad ya usada: motivo y vigencia ----------

  it('cuando el backend avisa que la actividad ya se usó, motivo y vigencia pasan a ser obligatorios', async () => {
    vi.mocked(actualizarPlanItem).mockRejectedValueOnce(
      new AppError('La actividad ya fue usada; indica el motivo y la fecha de vigencia de la nueva versión.',
        { code: 'SANIDAD_VERSION_MOTIVO_REQUERIDO' }))
    const { user, dialog } = await editar(itemManual())
    expect(dialog.getByLabelText(/^Motivo del cambio/)).not.toBeRequired()

    await user.click(dialog.getByRole('button', { name: 'Guardar cambios' }))

    expect(await dialog.findByText('Esta actividad ya se usó: indica el motivo del cambio.')).toBeInTheDocument()
    expect(dialog.getByText('Indica desde cuándo rige la nueva versión.')).toBeInTheDocument()
    expect(dialog.getByLabelText(/^Motivo del cambio/)).toBeRequired()
    expect(dialog.getByLabelText(/^Vigente desde/)).toBeRequired()

    await user.type(dialog.getByLabelText(/^Motivo del cambio/), 'Cambio de protocolo')
    fireEvent.change(dialog.getByLabelText(/^Vigente desde/), { target: { value: '2026-07-01T08:00' } })
    await user.click(dialog.getByRole('button', { name: 'Guardar cambios' }))

    await vi.waitFor(() => expect(actualizarPlanItem).toHaveBeenCalledTimes(2))
    expect(vi.mocked(actualizarPlanItem).mock.calls[1][3]).toMatchObject({ motivoVersion: 'Cambio de protocolo' })
    expect(vi.mocked(actualizarPlanItem).mock.calls[1][3].fechaVigencia).toBeDefined()
  })

})
