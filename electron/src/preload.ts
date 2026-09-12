import { contextBridge, ipcRenderer } from 'electron'

function readApiBaseUrl(): string | undefined {
  const flag = process.argv.find((arg) => arg.startsWith('--ganadero-api-base-url='))
  return flag?.slice('--ganadero-api-base-url='.length)
}

function readBackendStatus(): 'ok' | 'failed' {
  const flag = process.argv.find((arg) => arg.startsWith('--ganadero-backend-status='))
  return flag?.slice('--ganadero-backend-status='.length) === 'failed' ? 'failed' : 'ok'
}

contextBridge.exposeInMainWorld('ganadero', {
  apiBaseUrl: readApiBaseUrl(),
  quit: () => ipcRenderer.invoke('app:quit'),
  googleCalendar: {
    status: () => ipcRenderer.invoke('google-calendar-oauth:status'),
    importClientConfig: (jsonText: string, fileName: string) =>
      ipcRenderer.invoke('google-calendar-oauth:import-client', jsonText, fileName),
    connect: () => ipcRenderer.invoke('google-calendar-oauth:connect'),
    revoke: () => ipcRenderer.invoke('google-calendar-oauth:revoke'),
    changeAccount: () => ipcRenderer.invoke('google-calendar-oauth:change-account'),
    syncNow: () => ipcRenderer.invoke('google-calendar:sync-now'),
  },
  backups: {
    backendStatus: readBackendStatus(),
    getSettings: () => ipcRenderer.invoke('backups:get-settings'),
    saveSettings: (settings: unknown) => ipcRenderer.invoke('backups:save-settings', settings),
    selectExternalFolder: () => ipcRenderer.invoke('backups:select-external-folder'),
    createNow: () => ipcRenderer.invoke('backups:create-now'),
    copyExternal: (nombre: string) => ipcRenderer.invoke('backups:copy-external', nombre),
    list: () => ipcRenderer.invoke('backups:list'),
    verify: (nombre: string) => ipcRenderer.invoke('backups:verify', nombre),
    deleteBackup: (nombre: string) => ipcRenderer.invoke('backups:delete', nombre),
    selectRestoreFile: () => ipcRenderer.invoke('backups:select-restore-file'),
    inspectRestoreFile: (filePath: string) => ipcRenderer.invoke('backups:inspect-restore-file', filePath),
    restore: (filePath: string) => ipcRenderer.invoke('backups:restore', filePath),
    openLocalFolder: () => ipcRenderer.invoke('backups:open-local-folder'),
    openExternalFolder: () => ipcRenderer.invoke('backups:open-external-folder'),
  },
  sanidad: {
    exportarPlanilla: (input: unknown) => ipcRenderer.invoke('sanidad:exportar-planilla', input),
  },
  diagnostics: {
    export: () => ipcRenderer.invoke('diagnostics:export'),
    openLogsFolder: () => ipcRenderer.invoke('diagnostics:open-logs-folder'),
  },
})
