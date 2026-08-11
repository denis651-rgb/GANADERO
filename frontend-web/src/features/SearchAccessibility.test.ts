import { describe, expect, it } from 'vitest'
import animalesSource from '@/features/animales/pages/AnimalesPage.tsx?raw'
import lotesSource from '@/features/lotes/pages/LotesPage.tsx?raw'
import loteDetailSource from '@/features/lotes/pages/LoteDetailPage.tsx?raw'
import movimientosSource from '@/features/movimientos/pages/MovimientosPage.tsx?raw'

describe('accessible search names', () => {
  it('usa nombres específicos en todos los buscadores disponibles', () => {
    expect(animalesSource).toContain('aria-label="Buscar animales"')
    expect(lotesSource).toContain('aria-label="Buscar lotes"')
    expect(loteDetailSource).toContain('aria-label="Buscar animales para agregar al lote"')
    expect(movimientosSource).toContain('aria-label="Buscar animales para el movimiento"')
  })
})
