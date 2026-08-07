import { http } from '@/shared/api/http'
import type { ApiResponse } from '@/shared/api/types'

export interface Documento {
  id: string
  entidadTipo: string
  entidadId?: string
  nombreOriginal: string
  nombreAlmacenado: string
  mimeType: string
  tamanoBytes: number
  esPrincipal: boolean
  anchoPx?: number
  altoPx?: number
  createdBy: string
  usuarioNombre?: string
  version: number
  url: string
  createdAt: string
}

export async function listDocumentos(entidadTipo: string, entidadId: string) {
  return (await http.get<ApiResponse<Documento[]>>('/api/v1/archivos/documentos', { params: { entidadTipo, entidadId } })).data.data
}

export async function subirDocumento(file: File, entidadTipo: string, entidadId: string, principal: boolean) {
  const form = new FormData()
  form.append('file', file)
  form.append('entidadTipo', entidadTipo)
  form.append('entidadId', entidadId)
  form.append('principal', String(principal))
  return (await http.post<ApiResponse<Documento>>('/api/v1/archivos/documentos', form, { headers: { 'Content-Type': undefined } })).data.data
}

export async function marcarPrincipalDocumento(id: string) {
  return (await http.patch<ApiResponse<Documento>>(`/api/v1/archivos/documentos/${id}/principal`)).data.data
}

export async function eliminarDocumento(id: string, confirmarPrincipal: boolean) {
  await http.delete<ApiResponse<void>>(`/api/v1/archivos/documentos/${id}`, { params: { confirmarPrincipal } })
}
