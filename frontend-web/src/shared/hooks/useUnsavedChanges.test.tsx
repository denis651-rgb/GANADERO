import { useState } from 'react'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { UnsavedChangesDialog } from '@/shared/components/UnsavedChangesDialog'
import { useUnsavedChanges } from './useUnsavedChanges'

function Harness({ dirty }: { dirty: boolean }) {
  const [leaves, setLeaves] = useState(0)
  const unsaved = useUnsavedChanges(dirty)
  return <><button onClick={() => unsaved.requestLeave(() => setLeaves((value) => value + 1))}>Volver</button><output>{leaves}</output><UnsavedChangesDialog open={unsaved.open} onStay={unsaved.cancelLeave} onLeave={unsaved.discardAndLeave} /></>
}

describe('useUnsavedChanges', () => {
  it('permite salir sin preguntar cuando no hay cambios', async () => {
    render(<Harness dirty={false} />)
    await userEvent.click(screen.getByRole('button', { name: 'Volver' }))
    expect(screen.getByText('1')).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('pide confirmación con cambios y permite descartarlos', async () => {
    render(<Harness dirty />)
    await userEvent.click(screen.getByRole('button', { name: 'Volver' }))
    expect(screen.getByRole('dialog', { name: 'Tienes cambios sin guardar' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Salir sin guardar' }))
    expect(screen.getByText('1')).toBeInTheDocument()
  })

  it('deja de bloquear después de guardar o resetear', async () => {
    const { rerender } = render(<Harness dirty />)
    rerender(<Harness dirty={false} />)
    await userEvent.click(screen.getByRole('button', { name: 'Volver' }))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByText('1')).toBeInTheDocument()
  })
})
