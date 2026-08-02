export interface ApiResponse<T> {
  ok: boolean
  data: T
  timestamp: string
  correlationId: string
}

export interface ApiErrorBody {
  ok: false
  code: string
  message: string
  fieldErrors?: Array<{ field: string; message: string }>
  timestamp?: string
  correlationId?: string
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
