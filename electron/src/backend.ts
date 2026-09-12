import { app } from 'electron'
import { spawn, type ChildProcess } from 'node:child_process'
import { createServer } from 'node:net'
import path from 'node:path'
import fs from 'node:fs'
import { getLogsDir } from './logging'

const PREFERRED_PORT = 8080
const HEALTH_TIMEOUT_MS = 30_000

/** Busca un puerto TCP libre en 127.0.0.1, prefiriendo el puerto por defecto del backend. */
function findFreePort(preferred: number): Promise<number> {
  return new Promise((resolve, reject) => {
    const probe = createServer()
    probe.once('error', () => {
      const fallback = createServer()
      fallback.once('error', reject)
      fallback.listen(0, '127.0.0.1', () => {
        const address = fallback.address()
        const port = typeof address === 'object' && address ? address.port : preferred
        fallback.close(() => resolve(port))
      })
    })
    probe.once('listening', () => probe.close(() => resolve(preferred)))
    probe.listen(preferred, '127.0.0.1')
  })
}

function resolveJavaBinary(): string {
  if (!app.isPackaged) return 'java'
  const ext = process.platform === 'win32' ? 'java.exe' : 'java'
  return path.join(process.resourcesPath, 'jre', 'bin', ext)
}

function resolveJarPath(): string {
  if (app.isPackaged) return path.join(process.resourcesPath, 'backend', 'ganadero-backend.jar')
  const target = path.join(__dirname, '..', '..', 'backend', 'target')
  const jar = fs.readdirSync(target).find((name) => name.startsWith('ganadero-backend') && name.endsWith('.jar') && !name.endsWith('.original'))
  if (!jar) throw new Error(`No se encontró el jar del backend en ${target}. Corre "mvn package" en backend/ primero.`)
  return path.join(target, jar)
}

/**
 * spawn() en Windows no liga el ciclo de vida del hijo al del padre: si Electron se cierra
 * de forma abrupta (crash, kill), el java.exe del backend queda huérfano. Se registra el PID
 * en un archivo dentro de userData y, en cada arranque, se intenta terminar cualquier proceso
 * huérfano de una ejecución anterior antes de lanzar uno nuevo.
 */
function killOrphanFromPreviousRun(pidFile: string): void {
  if (!fs.existsSync(pidFile)) return
  const raw = fs.readFileSync(pidFile, 'utf8').trim()
  const pid = Number.parseInt(raw, 10)
  if (Number.isInteger(pid) && pid > 0) {
    try {
      process.kill(pid, 'SIGKILL')
    } catch {
      // ya no existe, no hay nada que limpiar
    }
  }
  fs.rmSync(pidFile, { force: true })
}

async function waitForHealth(port: number): Promise<void> {
  const deadline = Date.now() + HEALTH_TIMEOUT_MS
  while (Date.now() < deadline) {
    try {
      const response = await fetch(`http://127.0.0.1:${port}/actuator/health`)
      if (response.ok) return
    } catch {
      // el backend todavía no acepta conexiones
    }
    await new Promise((resolve) => setTimeout(resolve, 300))
  }
  throw new Error('El backend local no respondió dentro del tiempo esperado.')
}

export class BackendManager {
  private child: ChildProcess | null = null
  private pidFile: string | null = null
  port = PREFERRED_PORT

  get dbPath(): string {
    return path.join(app.getPath('userData'), 'ganadero.db')
  }

  get backupsPath(): string {
    return path.join(app.getPath('userData'), 'backups')
  }

  get mediaPath(): string {
    return path.join(app.getPath('userData'), 'media')
  }

  /** Misma carpeta que usan los logs de Electron (ver logging.ts): "Exportar información de
   * diagnóstico" empaqueta ambos juntos, como logs/ganadero-*.log + logs/backend-*.log. */
  get logsPath(): string {
    return getLogsDir()
  }

  async start(): Promise<number> {
    const userData = app.getPath('userData')
    fs.mkdirSync(userData, { recursive: true })
    this.pidFile = path.join(userData, 'backend.pid')
    killOrphanFromPreviousRun(this.pidFile)

    this.port = await findFreePort(PREFERRED_PORT)
    const javaBin = resolveJavaBinary()
    const jarPath = resolveJarPath()

    this.child = spawn(javaBin, ['-jar', jarPath], {
      env: {
        ...process.env,
        SPRING_PROFILES_ACTIVE: 'local',
        PORT: String(this.port),
        GANADERO_DB_PATH: this.dbPath,
        GANADERO_MEDIA_PATH: this.mediaPath,
        GANADERO_BACKUPS_PATH: this.backupsPath,
        GANADERO_LOGS_PATH: this.logsPath,
      },
      stdio: 'pipe',
      windowsHide: true,
    })
    if (this.child.pid) fs.writeFileSync(this.pidFile, String(this.child.pid))
    this.child.stdout?.on('data', (chunk) => console.log(`[backend] ${chunk}`.trimEnd()))
    this.child.stderr?.on('data', (chunk) => console.error(`[backend] ${chunk}`.trimEnd()))
    this.child.once('exit', (code) => {
      if (code !== null && code !== 0) console.error(`[backend] el proceso terminó con código ${code}`)
      this.child = null
    })

    await waitForHealth(this.port)
    return this.port
  }

  /**
   * Espera a que el proceso realmente termine (no solo a que se le mande la señal) antes de
   * resolver — imprescindible para restaurar: no se puede reemplazar ganadero.db mientras el
   * backend todavía lo tiene abierto. Si no termina solo, escala a SIGKILL tras 5s.
   */
  async stop(): Promise<void> {
    if (this.pidFile) fs.rmSync(this.pidFile, { force: true })
    const child = this.child
    if (!child) return
    await new Promise<void>((resolve) => {
      const timeout = setTimeout(() => child.kill('SIGKILL'), 5_000)
      child.once('exit', () => {
        clearTimeout(timeout)
        resolve()
      })
      child.kill()
    })
    this.child = null
  }

  async restart(): Promise<number> {
    await this.stop()
    return this.start()
  }
}
