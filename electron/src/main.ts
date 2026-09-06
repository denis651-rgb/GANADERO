import { app, BrowserWindow, Menu, Tray, dialog, ipcMain } from 'electron'
import path from 'node:path'
import { BackendManager } from './backend'
import { startNotificationPolling } from './notifications'
import { registerFrontendScheme, serveFrontend, FRONTEND_SCHEME } from './frontend-protocol'
import { GoogleOAuthManager } from './google-oauth'
import { startGoogleCalendarSync, type GoogleCalendarSyncHandle } from './google-calendar-sync'

const DEV_SERVER_URL = 'http://localhost:5173'
const ICON_PATH = path.join(__dirname, '..', 'build', 'icon.ico')

registerFrontendScheme()

const backend = new BackendManager()
const googleOAuth = new GoogleOAuthManager(() => backend.port)
let mainWindow: BrowserWindow | null = null
let tray: Tray | null = null
let stopNotifications: (() => void) | null = null
let googleCalendarSync: GoogleCalendarSyncHandle | null = null
let quitting = false

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
      dialog.showErrorBox('Ganadero', `No se pudo iniciar el servicio local:\n${(error as Error).message}`)
      app.quit()
      return
    }

    createWindow()
    registerGoogleOAuthIpc()
    createTray()
    stopNotifications = startNotificationPolling(() => backend.port, focusWindow)
    googleCalendarSync = startGoogleCalendarSync(() => backend.port, googleOAuth)
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

function createWindow(): void {
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 800,
    minWidth: 960,
    minHeight: 600,
    icon: ICON_PATH,
    show: false,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      additionalArguments: [`--ganadero-api-base-url=http://127.0.0.1:${backend.port}`],
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
  backend.stop()
})

app.on('activate', () => {
  if (!mainWindow) createWindow()
  else focusWindow()
})
