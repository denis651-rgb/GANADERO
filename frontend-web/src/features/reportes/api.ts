import { http } from '@/shared/api/http'
import type { ApiResponse } from '@/shared/api/types'
import type { ModalidadVenta } from '@/features/ventas/api'

export interface ReporteAnimalNacido {
  animalId: string
  codigo: string
  nombre?: string
  sexo: 'MACHO' | 'HEMBRA'
  fechaNacimiento: string
  fechaNacimientoEstimada: boolean
  raza: string
  categoria: string
  pesoNacimientoKg?: number
  propiedad?: string
  potrero?: string
  madre?: string
  padre?: string
}

export interface ReporteAnimalMuerto {
  animalId: string
  codigo: string
  nombre?: string
  sexo: 'MACHO' | 'HEMBRA'
  raza: string
  categoria: string
  fechaMuerte: string
  motivo?: string
  propiedad?: string
  potrero?: string
}

export interface ReporteVenta {
  ventaId: string
  animalId: string
  codigo: string
  nombre?: string
  raza: string
  fechaVenta: string
  comprador: string
  telefonoComprador?: string
  precio: number
  moneda: string
  pesoVentaKg?: number
  modalidad: ModalidadVenta
  precioUnitario?: number
}

export async function getReporteNacimientos(desde: string, hasta: string) {
  return (await http.get<ApiResponse<ReporteAnimalNacido[]>>('/api/v1/reportes/nacimientos', { params: { desde, hasta } })).data.data
}

export async function getReporteMuertes(desde: string, hasta: string) {
  return (await http.get<ApiResponse<ReporteAnimalMuerto[]>>('/api/v1/reportes/muertes', { params: { desde, hasta } })).data.data
}

export async function getReporteVentas(desde: string, hasta: string) {
  return (await http.get<ApiResponse<ReporteVenta[]>>('/api/v1/reportes/ventas', { params: { desde, hasta } })).data.data
}
