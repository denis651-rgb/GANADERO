import { http } from '@/shared/api/http'
import type { ApiResponse } from '@/shared/api/types'

export interface DashboardDistribucion {
  nombre: string
  total: number
}

export interface DashboardPesajeReciente {
  id: string
  animalId: string
  animalCodigo: string
  animalNombre?: string
  fecha: string
  pesoKg: number
}

export interface DashboardAlerta {
  tipo: string
  mensaje: string
  severidad: 'info' | 'warning' | 'danger'
  total: number
}

export interface DashboardResumen {
  totalAnimales: number
  animalesEnPotrero: number
  lotesActivos: number
  potrerosActivos: number
  pesoPromedioKg?: number
  gananciaPromedioKg?: number
  pesajesUltimos7Dias: number
  movimientosUltimos7Dias: number
  animalesSinPesaje: number
  animalesPorCategoria: DashboardDistribucion[]
  animalesPorPotrero: DashboardDistribucion[]
  animalesPorLote: DashboardDistribucion[]
  pesajesRecientes: DashboardPesajeReciente[]
  alertas: DashboardAlerta[]
  generadoEn: string
}

export async function getDashboardResumen() {
  const response = await http.get<ApiResponse<DashboardResumen>>('/api/v1/dashboard/resumen')
  return response.data.data
}
