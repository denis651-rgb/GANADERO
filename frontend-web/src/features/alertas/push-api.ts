import { http } from '@/shared/api/http'
import type { ApiResponse } from '@/shared/api/types'

export interface PushDevice { id:string; dispositivoNombre?:string; userAgent?:string; ultimoUsoAt?:string }
export interface NotificationPreferences { reproduccion:boolean;sanidad:boolean;tratamientos:boolean;pesajes:boolean;casosCriticos:boolean;criticas:boolean;urgentes:boolean;recordatorios:boolean }

const bytes = (value: string) => {
  const padding = '='.repeat((4 - value.length % 4) % 4)
  const raw = atob((value + padding).replace(/-/g, '+').replace(/_/g, '/'))
  return Uint8Array.from([...raw].map((char) => char.charCodeAt(0)))
}

export function pushSupported(){return 'serviceWorker'in navigator&&'PushManager'in window&&'Notification'in window}
export async function subscribePush(deviceName:string){
  if(!pushSupported())throw new Error('Este navegador no admite notificaciones Web Push.')
  const permission=await Notification.requestPermission();if(permission!=='granted')throw new Error('El permiso de notificaciones no fue concedido.')
  const [{data:keyResponse},registration]=await Promise.all([http.get<ApiResponse<{publicKey:string}>>('/api/v1/alertas/push/public-key'),navigator.serviceWorker.ready])
  if(!keyResponse.data.publicKey)throw new Error('La clave pública de notificaciones no está configurada.')
  let subscription=await registration.pushManager.getSubscription()
  if(!subscription)subscription=await registration.pushManager.subscribe({userVisibleOnly:true,applicationServerKey:bytes(keyResponse.data.publicKey)})
  const json=subscription.toJSON();return(await http.post<ApiResponse<PushDevice>>('/api/v1/alertas/push/suscripciones',{endpoint:json.endpoint,keys:json.keys,dispositivoNombre:deviceName})).data.data
}
export async function listPushDevices(){return(await http.get<ApiResponse<PushDevice[]>>('/api/v1/alertas/push/suscripciones')).data.data}
export async function unsubscribePush(id:string){await http.delete(`/api/v1/alertas/push/suscripciones/${id}`);const registration=await navigator.serviceWorker.ready;await(await registration.pushManager.getSubscription())?.unsubscribe()}
export async function getNotificationPreferences(){return(await http.get<ApiResponse<NotificationPreferences>>('/api/v1/alertas/configuracion')).data.data}
export async function saveNotificationPreferences(value:NotificationPreferences){return(await http.put<ApiResponse<NotificationPreferences>>('/api/v1/alertas/configuracion',value)).data.data}
