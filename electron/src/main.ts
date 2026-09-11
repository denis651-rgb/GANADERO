import { app, BrowserWindow, Menu, Tray, ipcMain } from 'electron'
import path from 'node:path'
import { BackendManager } from './backend'
import { BackupManager } from './backups/manager'
import { startBackupScheduler, type BackupSchedulerHandle } from './backups/scheduler'
import { startNotificationPolling } from './notifications'
import { registerFrontendScheme, serveFrontend, FRONTEND_SCHEME } from './frontend-protocol'
import { GoogleOAuthManager } from './google-oauth'
import { startGoogleCalendarSync, type GoogleCalendarSyncHandle } from './google-calendar-sync'

const DEV_SERVER_URL = 'http://localhost:5173'
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
    registerAppIpc()
    createTray()

    if (!backendFailed) {
      registerGoogleOAuthIpc()
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
  ipcMain.handle('google-calendar:sync-now', () => googleCalendarSync?.syncNow())
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
