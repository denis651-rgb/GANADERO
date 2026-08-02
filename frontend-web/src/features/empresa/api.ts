import { http } from '@/shared/api/http'
import type { ApiResponse } from '@/shared/api/types'

export interface Empresa {
  id: string
  codigo: string
  razonSocial: string
  nombreComercial: string
  nit?: string
  telefono?: string
  email?: string
  direccion?: string
  moneda: string
  estado: string
  version: number
}

export interface ConfiguracionEmpresa {
  unidadPeso: 'KG'
  unidadSuperficie: 'HA'
  moneda: string
  diasAlertaPreparto: number
  diasAlertaVacunacion: number
  diasSinPesaje: number
  permitirStockNegativo: boolean
  requiereAprobacionVenta: boolean
  comprimirImagenes: boolean
  calidadImagen: number
  version: number
}

export async function getEmpresa() {
  return (await http.get<ApiResponse<Empresa>>('/api/v1/empresa')).data.data
}

export async function updateEmpresa(input: Partial<Empresa> & { version: number }) {
  return (await http.patch<ApiResponse<Empresa>>('/api/v1/empresa', input)).data.data
}

export async function getConfiguracionEmpresa() {
  return (await http.get<ApiResponse<ConfiguracionEmpresa>>('/api/v1/empresa/configuracion')).data.data
}

export async function updateConfiguracion(input: Partial<ConfiguracionEmpresa> & { version: number }) {
  return (await http.patch<ApiResponse<ConfiguracionEmpresa>>('/api/v1/empresa/configuracion', input)).data.data
}
