import { BrowserWindow } from 'electron'
import path from 'node:path'

let splashWindow: BrowserWindow | null = null

const SPLASH_PATH = path.join(__dirname, '..', 'build', 'splash.html')

/** Muestra la ventana de bienvenida mientras arranca el backend y carga el frontend. */
export function createSplash(): void {
  if (splashWindow) return

  splashWindow = new BrowserWindow({
    width: 460,
    height: 520,
    frame: false,
    transparent: true,
    resizable: false,
    movable: true,
    minimizable: false,
    maximizable: false,
    fullscreenable: false,
    skipTaskbar: true,
    alwaysOnTop: true,
    center: true,
    show: false,
    webPreferences: { sandbox: true },
  })

  splashWindow.setAlwaysOnTop(true, 'pop-up-menu')
  void splashWindow.loadFile(SPLASH_PATH)
  splashWindow.once('ready-to-show', () => splashWindow?.show())
  splashWindow.on('closed', () => { splashWindow = null })
}

/** Cierra la ventana de bienvenida (usar cuando la ventana principal esté lista). */
export function closeSplash(): void {
  if (!splashWindow) return
  splashWindow.destroy()
  splashWindow = null
}