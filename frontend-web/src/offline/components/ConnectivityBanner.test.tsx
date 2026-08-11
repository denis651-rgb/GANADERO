import { render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ConnectivityBanner } from './ConnectivityBanner'

let online = true
vi.mock('@/shared/hooks/useOnlineStatus', () => ({ useOnlineStatus: () => online }))

describe('ConnectivityBanner announcements', () => {
  it('anuncia una sola vez la desconexión y la reconexión', async () => {
    const view = render(<ConnectivityBanner />)
    const status = screen.getByRole('status')
    expect(status).toHaveTextContent('')

    online = false
    view.rerender(<ConnectivityBanner />)
    await waitFor(() => expect(status).toHaveTextContent('Sin conexión. Puedes continuar trabajando. Los cambios se sincronizarán cuando vuelva internet.'))

    online = true
    view.rerender(<ConnectivityBanner />)
    await waitFor(() => expect(status).toHaveTextContent('Conexión restablecida.'))
  })
})
