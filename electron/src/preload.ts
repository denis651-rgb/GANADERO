import { contextBridge, ipcRenderer } from 'electron'

function readApiBaseUrl(): string | undefined {
  const flag = process.argv.find((arg) => arg.startsWith('--ganadero-api-base-url='))
  return flag?.slice('--ganadero-api-base-url='.length)
}

contextBridge.exposeInMainWorld('ganadero', {
  apiBaseUrl: readApiBaseUrl(),
  googleCalendar: {
    status: () => ipcRenderer.invoke('google-calendar-oauth:status'),
    importClientConfig: (jsonText: string, fileName: string) =>
      ipcRenderer.invoke('google-calendar-oauth:import-client', jsonText, fileName),
    connect: () => ipcRenderer.invoke('google-calendar-oauth:connect'),
    revoke: () => ipcRenderer.invoke('google-calendar-oauth:revoke'),
    changeAccount: () => ipcRenderer.invoke('google-calendar-oauth:change-account'),
    syncNow: () => ipcRenderer.invoke('google-calendar:sync-now'),
  },
})
