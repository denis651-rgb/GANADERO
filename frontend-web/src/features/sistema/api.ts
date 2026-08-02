import { http } from '@/shared/api/http'
import type { ApiResponse } from '@/shared/api/types'

export interface SystemStatus {
  application: string
  architecture: string
  phase: string
  moduleCount: number
  modules: string[]
}

export async function getSystemStatus() {
  const response = await http.get<ApiResponse<SystemStatus>>('/api/v1/system/status')
  return response.data.data
}
