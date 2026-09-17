import { app, BrowserWindow, Menu, Tray, ipcMain, shell } from 'electron'
import path from 'node:path'
import { BackendManager } from './backend'
import { BackupManager } from './backups/manager'
import { startBackupScheduler, type BackupSchedulerHandle } from './backups/scheduler'
import { exportarDiagnostico } from './diagnostics'
import { initLogging, getLogsDir } from './logging'
import { startNotificationPolling } from './notifications'
import { registerFrontendScheme, serveFrontend, FRONTEND_SCHEME } from './frontend-protocol'
import { GoogleOAuthManager } from './google-oauth'
import { startGoogleCalendarSync, type GoogleCalendarSyncHandle } from './google-calendar-sync'
import { exportarPlanillaSanitaria, type PlanillaSanitariaInput } from './sanidad-export'
import { buscarActualizacionesManualmente, initAutoUpdater } from './updater'
import { exportManualPdf } from './manual-export'
import { DEV_SERVER_URL } from './constants'
const ICON_PATH = path.join(__dirname, '..', 'build', 'icon.ico')

registerFrontendScheme()

const backend = new BackendManager()
const backupManager = new BackupManager(backend)
const googleOAuth = new GoogleOAuthManager(() => backend.port)
let mainWindow: BrowserWindow | null = null
let tray: Tray | null = null
let stopNotifications: (() => void) | null = null
let googleCalendarSync: GoogleCalendarSyncHandle | null = null
let backupScheduler: BackupSchedulerHandle | null = null
let quitting = false
let backendFailed = false
let backendRestartAttempt = 0

const BACKEND_RESTART_DELAYS_MS = [2_000, 5_000, 10_000]

function sendBackendStatus(status: { state: 'reconnecting' | 'restored' | 'failed'; attempt?: number; maxAttempts?: number }): void {
  mainWindow?.webContents.send('backend:status', status)
}

/**
 * El backend ya había arrancado bien y murió solo (crash, o alguien mató el proceso java a
 * mano/con otra herramienta — nos pasó durante desarrollo). Reintenta con backoff en vez de
 * dejar la ventana abierta pegándole en silencio a un backend que ya no existe.
 */
async function handleBackendCrash(): Promise<void> {
  if (quitting) return
  backendRestartAttempt += 1
  const maxAttempts = BACKEND_RESTART_DELAYS_MS.length
  sendBackendStatus({ state: 'reconnecting', attempt: backendRestartAttempt, maxAttempts })

  if (backendRestartAttempt > maxAttempts) {
    sendBackendStatus({ state: 'failed' })
    return
  }

  await new Promise((resolve) => setTimeout(resolve, BACKEND_RESTART_DELAYS_MS[backendRestartAttempt - 1]))
  if (quitting) return
  try {
    await backend.start()
    backendRestartAttempt = 0
    sendBackendStatus({ state: 'restored' })
  } catch (error) {
    console.error('[main] reintento de arranque del backend falló:', error)
    void handleBackendCrash()
  }
}

if (!app.requestSingleInstanceLock()) {
  app.quit()
} else {
  app.on('second-instance', () => {
    if (!mainWindow) return
    if (mainWindow.isMinimized()) mainWindow.restore()
    mainWindow.show()
    mainWindow.focus()
  })

  app.setName('Ganadero')
  // app.setName() no siempre alcanza a tiempo para app.getPath('userData'); se fija explícito
  // para garantizar %APPDATA%/Ganadero como pide el plan de escritorio.
  app.setPath('userData', path.join(app.getPath('appData'), 'Ganadero'))
  // Debe ir después de fijar userData: initLogging() calcula la carpeta de logs a partir de
  // app.getPath('userData'), y tiene que coincidir con la que usa BackendManager para db/media/backups.
  initLogging()

  app.whenReady().then(async () => {
    serveFrontend()
    try {
      await backend.start()
    } catch (error) {
      // No se cierra la app: se abre en modo recuperación para que el usuario pueda restaurar
      // un respaldo sin depender de que el backend (que es justo lo que falló) esté disponible.
      console.error('[main] el backend no pudo iniciar:', error)
      backendFailed = true
    }

    createWindow(backendFailed)
    registerBackupsIpc()
    registerDiagnosticsIpc()
    registerAppIpc()
    registerManualIpc()
    try {
      createTray()
    } catch (error) {
      // No debe tumbar el resto del arranque (notificaciones, respaldos, updater): sin bandeja
      // la app pierde el ícono y el menú de "Salir", pero sigue siendo usable desde la ventana.
      console.error('[main] no se pudo crear el ícono de la bandeja:', error)
    }
    // Se busca incluso en modo recuperación: si el backend no arrancó, una actualización podría
    // ser justo el arreglo.
    initAutoUpdater()

    if (!backendFailed) {
      backend.onUnexpectedExit = () => void handleBackendCrash()
      registerGoogleOAuthIpc()
      registerSanidadIpc()
      stopNotifications = startNotificationPolling(() => backend.port, focusWindow)
      googleCalendarSync = startGoogleCalendarSync(() => backend.port, googleOAuth)
      backupScheduler = startBackupScheduler(backupManager)
    }
  })
}

/** Usado por la pantalla de recuperación: window.close() del renderer solo oculta la ventana a la bandeja. */
function registerAppIpc(): void {
  ipcMain.handle('app:quit', () => {
    quitting = true
    app.quit()
  })
}

function registerGoogleOAuthIpc(): void {
  ipcMain.handle('google-calendar-oauth:status', () => googleOAuth.status())
  ipcMain.handle('google-calendar-oauth:connect', () => googleOAuth.connect())
  ipcMain.handle('google-calendar-oauth:import-client', (_event, jsonText: string, fileName: string) =>
    googleOAuth.importClientConfig(jsonText, fileName))
  ipcMain.handle('google-calendar-oauth:revoke', () => googleOAuth.revoke())
  ipcMain.handle('google-calendar-oauth:change-account', () => googleOAuth.changeAccount())
  ipcMain.handle('google-calendar-oauth:cancel', () => googleOAuth.cancelConnect())
  ipcMain.handle('google-calendar:sync-now', () => googleCalendarSync?.syncNow())
}

function registerSanidadIpc(): void {
  ipcMain.handle('sanidad:exportar-planilla', (_event, input: PlanillaSanitariaInput) => exportarPlanillaSanitaria(input))
}

/** Disponible incluso si el backend falló al iniciar: es justo cuando más útil es poder exportar los logs. */
function registerDiagnosticsIpc(): void {
  ipcMain.handle('diagnostics:export', () => exportarDiagnostico(backend))
  ipcMain.handle('diagnostics:open-logs-folder', () => shell.openPath(getLogsDir()))
}

/** El PDF es contenido estático del manual, no depende de que el backend local haya arrancado. */
function registerManualIpc(): void {
  ipcMain.handle('manual:export-pdf', () => exportManualPdf(() => backend.port))
}

/**
 * Funciona con o sin backend arriba: list()/verify() de BackupManager caen a leer los archivos
 * directamente de la carpeta local si el backend no responde — es lo que permite operar estos
 * canales también desde la ventana de recuperación.
 */
function registerBackupsIpc(): void {
  ipcMain.handle('backups:get-settings', () => backupManager.getSettings())
  ipcMain.handle('backups:save-settings', (_event, settings) => backupManager.saveSettings(settings))
  ipcMain.handle('backups:select-external-folder', () => backupManager.selectExternalFolder())
  ipcMain.handle('backups:create-now', () => backupManager.createNow())
  ipcMain.handle('backups:copy-external', (_event, nombre: string) => backupManager.copyToExternal(nombre))
  ipcMain.handle('backups:list', () => backupManager.list())
  ipcMain.handle('backups:verify', (_event, nombre: string) => backupManager.verify(nombre))
  ipcMain.handle('backups:delete', (_event, nombre: string) => backupManager.deleteBackup(nombre))
  ipcMain.handle('backups:select-restore-file', () => backupManager.selectRestoreFile())
  ipcMain.handle('backups:inspect-restore-file', (_event, filePath: string) => backupManager.inspectRestoreFile(filePath))
  ipcMain.handle('backups:restore', (_event, filePath: string) => backupManager.restore(filePath))
  ipcMain.handle('backups:open-local-folder', () => backupManager.openLocalFolder())
  ipcMain.handle('backups:open-external-folder', () => backupManager.openExternalFolder())
}

function createWindow(backendFailed: boolean): void {
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 800,
    minWidth: 960,
    minHeight: 600,
    icon: ICON_PATH,
    show: false,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      additionalArguments: [
        `--ganadero-api-base-url=http://127.0.0.1:${backend.port}`,
        `--ganadero-backend-status=${backendFailed ? 'failed' : 'ok'}`,
      ],
      contextIsolation: true,
      nodeIntegration: false,
    },
  })

  mainWindow.once('ready-to-show', () => mainWindow?.show())

  if (app.isPackaged) {
    void mainWindow.loadURL(`${FRONTEND_SCHEME}://index.html`)
  } else {
    void mainWindow.loadURL(DEV_SERVER_URL)
  }

  mainWindow.on('close', (event) => {
    if (quitting) return
    event.preventDefault()
    mainWindow?.hide()
  })

  mainWindow.on('closed', () => { mainWindow = null })
}

function createTray(): void {
  tray = new Tray(ICON_PATH)
  tray.setToolTip('Ganadero')
  tray.setContextMenu(Menu.buildFromTemplate([
    { label: 'Abrir Ganadero', click: focusWindow },
    { label: 'Buscar actualizaciones', click: buscarActualizacionesManualmente },
    { type: 'separator' },
    { label: 'Salir', click: () => { quitting = true; app.quit() } },
  ]))
  tray.on('double-click', focusWindow)
}

function focusWindow(): void {
  if (!mainWindow) return
  if (mainWindow.isMinimized()) mainWindow.restore()
  mainWindow.show()
  mainWindow.focus()
}

app.on('window-all-closed', () => {
  // La app sigue viva en la bandeja para poder notificar alertas aunque se cierre la ventana.
})

app.on('before-quit', () => {
  quitting = true
  stopNotifications?.()
  googleCalendarSync?.stop()
  backupScheduler?.stop()
  void backend.stop()
})

app.on('activate', () => {
  if (!mainWindow) createWindow(backendFailed)
  else focusWindow()
})
