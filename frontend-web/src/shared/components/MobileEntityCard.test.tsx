import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import userEvent from '@testing-library/user-event'
import { MobileEntityCard } from './MobileEntityCard'

describe('MobileEntityCard', () => {
  it('conserva la información crítica y una acción accesible', async () => {
    const onOpen = vi.fn()
    render(<MobileEntityCard title="A-00142" subtitle="Lola" status="ACTIVO" metadata={<><span>Vaca · Hembra</span><span>Potrero Norte</span></>} action={<button onClick={onOpen}>Ver animal</button>} />)

    expect(screen.getByRole('article')).toHaveTextContent('A-00142')
    expect(screen.getByRole('article')).toHaveTextContent('Potrero Norte')
    await userEvent.click(screen.getByRole('button', { name: 'Ver animal' }))
    expect(onOpen).toHaveBeenCalledOnce()
  })
})
