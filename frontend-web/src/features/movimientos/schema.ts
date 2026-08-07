import { z } from 'zod'

const backendUuid = (message: string) => z.string().regex(
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
  message,
)

const optionalUuid = (message: string) => z.union([z.literal(''), backendUuid(message)]).optional()
  .transform((value) => (value === '' ? undefined : value))

const optionalText = (max: number) => z.union([z.literal(''), z.string().trim().max(max)]).optional()
  .transform((value) => (value === '' ? undefined : value))

export const crearMovimientoSchema = z.object({
  tipo: z.enum(['CAMBIO_POTRERO', 'CAMBIO_LOTE', 'TRANSFERENCIA_PROPIEDAD', 'INGRESO_COMPRA', 'SALIDA_VENTA', 'CUARENTENA', 'RETORNO_CUARENTENA']),
  fecha: optionalText(10),
  motivo: optionalText(1000),
  observacion: optionalText(1000),
  origenPropiedadId: optionalUuid('Selecciona una propiedad.'),
  origenPotreroId: optionalUuid('Selecciona un potrero.'),
  origenLoteId: optionalUuid('Selecciona un lote.'),
  destinoPropiedadId: optionalUuid('Selecciona una propiedad.'),
  destinoPotreroId: optionalUuid('Selecciona un potrero.'),
  destinoLoteId: optionalUuid('Selecciona un lote.'),
  animales: z.array(
    z.object({
      animalId: backendUuid('Selecciona un animal.'),
      version: z.coerce.number().min(0),
    }),
  ).min(1, 'Selecciona al menos un animal.'),
}).superRefine((data, ctx) => {
  const requerido = (destino: string, message: string) => {
    ctx.addIssue({ code: z.ZodIssueCode.custom, path: [destino], message })
  }
  switch (data.tipo) {
    case 'CAMBIO_POTRERO':
    case 'CUARENTENA':
    case 'RETORNO_CUARENTENA':
      if (!data.destinoPotreroId) requerido('destinoPotreroId', 'El potrero de destino es requerido.')
      break
    case 'CAMBIO_LOTE':
      if (!data.destinoLoteId) requerido('destinoLoteId', 'El lote de destino es requerido.')
      break
    case 'INGRESO_COMPRA':
    case 'TRANSFERENCIA_PROPIEDAD':
      if (!data.destinoPropiedadId) requerido('destinoPropiedadId', 'La propiedad de destino es requerida.')
      break
    case 'SALIDA_VENTA':
      break
  }
  if (data.tipo === 'TRANSFERENCIA_PROPIEDAD' && data.origenPropiedadId && data.destinoPropiedadId && data.origenPropiedadId === data.destinoPropiedadId) {
    ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['destinoPropiedadId'], message: 'La propiedad de destino debe ser distinta de la de origen.' })
  }
  if (data.tipo === 'CAMBIO_POTRERO' && data.origenPotreroId && data.destinoPotreroId && data.origenPotreroId === data.destinoPotreroId) {
    ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['destinoPotreroId'], message: 'El potrero de destino debe ser distinto del origen.' })
  }
})

export const anularMovimientoSchema = z.object({
  motivo: z.string().trim().min(3, 'Indica el motivo de anulación.').max(1000, 'El motivo es demasiado largo.'),
})

export const revertirMovimientoSchema = z.object({
  motivo: z.string().trim().min(3, 'Indica el motivo de la reversión.').max(1000, 'El motivo es demasiado largo.'),
})

export type CrearMovimientoFormInput = z.input<typeof crearMovimientoSchema>
export type CrearMovimientoForm = z.output<typeof crearMovimientoSchema>
export type AnularMovimientoForm = z.infer<typeof anularMovimientoSchema>
export type RevertirMovimientoForm = z.infer<typeof revertirMovimientoSchema>
