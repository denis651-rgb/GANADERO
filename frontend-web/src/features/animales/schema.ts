import { z } from 'zod'

const backendUuid = (message: string) => z.string().regex(
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
  message,
)

export const createAnimalSchema = z.object({
  nombre: z.string().trim().max(100).optional(),
  sexo: z.enum(['MACHO', 'HEMBRA']),
  fechaNacimiento: z.string().optional(),
  proposito: z.enum(['CARNE', 'LECHE', 'REPRODUCCION', 'DOBLE_PROPOSITO']),
  origen: z.enum(['NACIDO', 'COMPRADO', 'TRANSFERIDO']),
  razaPrincipalId: backendUuid('Selecciona una raza válida.'),
  categoriaActualId: backendUuid('Selecciona una categoría válida.'),
  propiedadActualId: backendUuid('Selecciona una propiedad válida.'),
  potreroActualId: backendUuid('Selecciona un potrero válido.'),
  observaciones: z.string().trim().max(1000).optional(),
})
