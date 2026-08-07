import { http } from '@/shared/api/http'
import type { ApiResponse, Page } from '@/shared/api/types'
import type { AnimalEvent, AnimalFilters, AnimalState, AnimalSummary, ActualizarIdentificadorInput, AsignarIdentificadorInput, CategoriaAnimal, CreateAnimalInput, CrearParentescoInput, Identificador, Parentesco, Raza, TimelineEvent, UpdateAnimalInput } from '@/features/animales/types'

export async function listAnimals(filters: AnimalFilters) {
  const response = await http.get<ApiResponse<Page<AnimalSummary>>>('/api/v1/animales', {
    params: {
      search: filters.search || undefined,
      estado: filters.estado || undefined,
      propiedadId: filters.propiedadId || undefined,
      potreroId: filters.potreroId || undefined,
      categoria: filters.categoria || undefined,
      sexo: filters.sexo || undefined,
      page: filters.page,
      size: filters.size,
    },
  })
  return response.data.data
}

export async function getAnimal(id: string) { return (await http.get<ApiResponse<AnimalSummary>>(`/api/v1/animales/${id}`)).data.data }
export async function getAnimalHistory(id: string) { return (await http.get<ApiResponse<AnimalEvent[]>>(`/api/v1/animales/${id}/historial`)).data.data }
export async function updateAnimal(id: string, input: UpdateAnimalInput) { return (await http.patch<ApiResponse<AnimalSummary>>(`/api/v1/animales/${id}`, input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data }
export async function changeAnimalState(id: string, estado: AnimalState, motivo: string, version: number) { return (await http.patch<ApiResponse<AnimalSummary>>(`/api/v1/animales/${id}/estado`, { estado, motivo, version }, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data }

export async function createAnimal(input: CreateAnimalInput) {
  const response = await http.post<ApiResponse<AnimalSummary>>('/api/v1/animales', input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })
  return response.data.data
}

export async function listRazas() { return (await http.get<ApiResponse<Raza[]>>('/api/v1/razas')).data.data }
export async function listCategorias() { return (await http.get<ApiResponse<CategoriaAnimal[]>>('/api/v1/categorias-animal')).data.data }

export async function getAnimalTimeline(id: string, params: { tipo?: string; modulo?: string; desde?: string; hasta?: string; page: number; size: number }) {
  return (await http.get<ApiResponse<Page<TimelineEvent>>>(`/api/v1/animales/${id}/timeline`, { params })).data.data
}

export async function listIdentificadores(animalId: string) { return (await http.get<ApiResponse<Identificador[]>>(`/api/v1/animales/${animalId}/identificadores`)).data.data }
export async function asignarIdentificador(animalId: string, input: AsignarIdentificadorInput) { return (await http.post<ApiResponse<Identificador>>(`/api/v1/animales/${animalId}/identificadores`, input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data }
export async function actualizarIdentificador(animalId: string, identificadorId: string, input: ActualizarIdentificadorInput) { return (await http.patch<ApiResponse<Identificador>>(`/api/v1/animales/${animalId}/identificadores/${identificadorId}`, input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data }
export async function retirarIdentificador(animalId: string, identificadorId: string, motivo: string, version: number) { return (await http.post<ApiResponse<Identificador>>(`/api/v1/animales/${animalId}/identificadores/${identificadorId}/retirar`, { motivo, version }, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data }
export async function hacerPrincipalIdentificador(animalId: string, identificadorId: string, version: number) { return (await http.post<ApiResponse<Identificador>>(`/api/v1/animales/${animalId}/identificadores/${identificadorId}/principal`, { version }, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data }

export async function listParentescos(animalId: string) { return (await http.get<ApiResponse<Parentesco[]>>(`/api/v1/animales/${animalId}/parentescos`)).data.data }
export async function crearParentesco(animalId: string, input: CrearParentescoInput) { return (await http.post<ApiResponse<Parentesco>>(`/api/v1/animales/${animalId}/parentescos`, input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data }
export async function eliminarParentesco(animalId: string, parentescoId: string) { return (await http.delete<ApiResponse<void>>(`/api/v1/animales/${animalId}/parentescos/${parentescoId}`, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data }
