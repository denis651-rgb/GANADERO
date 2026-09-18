import { z } from 'zod'

const backendUuid = (message: string) => z.string().regex(
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
  message,
)

export const createAnimalSchema = z.object({
  nombre: z.string().trim().max(100).optional(),
  sexo: z.enum(['MACHO', 'HEMBRA']),
  fechaNacimiento: z.string().optional(),
  fechaNacimientoEstimada: z.boolean().optional(),
  fechaIngreso: z.string().optional(),
  color: z.string().trim().max(60).optional(),
  pesoNacimientoKg: z.number().nonnegative('El peso al nacer no puede ser negativo.').optional(),
  condicionCorporalActual: z.number().min(1, 'Mínimo 1.').max(5, 'Máximo 5.').optional(),
  pesoIngresoKg: z.number().positive('El peso debe ser mayor que cero.').optional(),
  pesoIngresoEstimado: z.boolean().optional(),
  proposito: z.enum(['CARNE', 'LECHE', 'REPRODUCCION', 'DOBLE_PROPOSITO']),
  origen: z.enum(['NACIDO', 'COMPRADO', 'TRANSFERIDO']),
  razaPrincipalId: backendUuid('Selecciona una raza válida.'),
  categoriaActualId: backendUuid('Selecciona una categoría válida.'),
  categoriaManualMotivo: z.string().trim().max(300).optional(),
  propiedadActualId: backendUuid('Selecciona una propiedad válida.'),
  potreroActualId: backendUuid('Selecciona un potrero válido.'),
  observaciones: z.string().trim().max(1000).optional(),
  edadDeclaradaValor: z.number().int().positive('La edad aproximada debe ser mayor que cero.').optional(),
  edadDeclaradaUnidad: z.enum(['DIAS', 'MESES', 'ANIOS']).optional(),
  fechaReferenciaEdad: z.string().optional(),
  fuenteEdad: z.enum(['PROVEEDOR', 'ESTIMACION_CAMPO']).optional(),
  observacionEstimacion: z.string().trim().max(500).optional(),
}).superRefine((animal, context) => {
  if (animal.origen === 'NACIDO' && !animal.fechaNacimiento && !animal.edadDeclaradaValor) {
    context.addIssue({ code: 'custom', path: ['fechaNacimiento'], message: 'Indica la fecha o la edad aproximada del animal nacido en la finca.' })
  }
  if (animal.origen !== 'NACIDO' && !animal.fechaIngreso) {
    context.addIssue({ code: 'custom', path: ['fechaIngreso'], message: 'Indica la fecha de recepción del animal.' })
  }
  if (animal.fechaNacimiento && animal.fechaIngreso && animal.origen !== 'NACIDO' && animal.fechaIngreso < animal.fechaNacimiento) {
    context.addIssue({ code: 'custom', path: ['fechaIngreso'], message: 'La recepción no puede ser anterior al nacimiento.' })
  }
  if (animal.edadDeclaradaValor && (!animal.edadDeclaradaUnidad || !animal.fechaReferenciaEdad || !animal.fuenteEdad)) {
    context.addIssue({ code: 'custom', path: ['edadDeclaradaValor'], message: 'Completa unidad, referencia y fuente de la edad aproximada.' })
  }
  if (animal.edadDeclaradaValor && animal.fechaNacimiento) {
    context.addIssue({ code: 'custom', path: ['edadDeclaradaValor'], message: 'Usa fecha conocida o edad aproximada, no ambas.' })
  }
})
