import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ManualViewer } from './ManualViewer'
import type { ManualChapter } from '@/features/manual/manualRegistry'

const showToast = vi.fn()
vi.mock('@/shared/toast/useToast', () => ({ useToast: () => ({ showToast }) }))

beforeEach(() => { showToast.mockClear() })

function chapter(content: string): ManualChapter {
  return { id: 'prueba', title: 'Prueba', file: 'prueba.md', content }
}

function renderChapter(content: string) {
  return render(<MemoryRouter><ManualViewer chapter={chapter(content)} /></MemoryRouter>)
}

describe('ManualViewer', () => {
  it('agrega un id estable a cada encabezado', () => {
    renderChapter('## Compra individual')
    const heading = screen.getByRole('heading', { level: 2, name: /Compra individual/ })
    expect(heading).toHaveAttribute('id', 'compra-individual')
  })

  it('copia el enlace de la sección al portapapeles y avisa con un toast', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined)
    Object.assign(navigator, { clipboard: { writeText } })
    renderChapter('## Compra individual')

    fireEvent.click(screen.getByRole('button', { name: 'Copiar enlace a esta sección' }))

    expect(writeText).toHaveBeenCalledWith('/manual/prueba#compra-individual')
    await vi.waitFor(() => expect(showToast).toHaveBeenCalledWith('Enlace de la sección copiado.'))
  })

  it('si falla el portapapeles, avisa en vez de romper el clic', async () => {
    const writeText = vi.fn().mockRejectedValue(new Error('denegado'))
    Object.assign(navigator, { clipboard: { writeText } })
    renderChapter('## Compra individual')

    fireEvent.click(screen.getByRole('button', { name: 'Copiar enlace a esta sección' }))

    await vi.waitFor(() => expect(showToast).toHaveBeenCalledWith('No se pudo copiar el enlace.', 'danger'))
  })

  it('sin API de portapapeles disponible, avisa sin romper el clic', () => {
    Object.assign(navigator, { clipboard: undefined })
    renderChapter('## Compra individual')

    fireEvent.click(screen.getByRole('button', { name: 'Copiar enlace a esta sección' }))

    expect(showToast).toHaveBeenCalledWith('No se pudo copiar el enlace.', 'danger')
  })

  it('abre la imagen en grande al hacer clic y la cierra', () => {
    renderChapter('![Formulario de compra](./compra.png)')
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Formulario de compra' }))

    const dialog = screen.getByRole('dialog', { name: 'Formulario de compra' })
    expect(dialog.querySelector('img')).toHaveAttribute('src', './compra.png')
  })

  it('un enlace externo abre en pestaña nueva', () => {
    renderChapter('[Google](https://google.com)')
    expect(screen.getByRole('link', { name: 'Google' })).toHaveAttribute('target', '_blank')
  })

  it('un enlace a otro capítulo navega dentro de la app, no como archivo relativo', () => {
    renderChapter('[otro capítulo](./04-compras.md#compra-individual)')
    const link = screen.getByRole('link', { name: 'otro capítulo' })
    expect(link).toHaveAttribute('href', '/manual/compras#compra-individual')
    expect(link).not.toHaveAttribute('target')
  })

  it('una cita se muestra como nota, no como cita textual', () => {
    renderChapter('> **Importante:** esto es una nota.')
    const note = screen.getByText(/esto es una nota/).closest('blockquote')
    expect(note).toHaveClass('manual-note')
  })

  it('una tabla queda envuelta en un contenedor con scroll horizontal', () => {
    renderChapter('| A | B |\n| --- | --- |\n| 1 | 2 |')
    const table = screen.getByRole('table')
    expect(table.parentElement).toHaveClass('manual-table-wrapper')
  })

  describe('secciones plegables (forma B)', () => {
    const contenidoConOperacion = '## Plan sanitario\n\n### Para qué sirve\n\nAgrupa actividades.\n\n### Procedimiento\n\n1. Paso uno.'

    it('una sección con sub-encabezados empieza plegada', () => {
      renderChapter(contenidoConOperacion)
      expect(screen.getByRole('button', { name: /Plan sanitario/ })).toHaveAttribute('aria-expanded', 'false')
      expect(screen.queryByText('Agrupa actividades.')).not.toBeVisible()
    })

    it('un clic en el encabezado despliega la sección, y otro clic la vuelve a plegar', () => {
      renderChapter(contenidoConOperacion)
      const toggle = screen.getByRole('button', { name: /Plan sanitario/ })

      fireEvent.click(toggle)
      expect(toggle).toHaveAttribute('aria-expanded', 'true')
      expect(screen.getByText('Agrupa actividades.')).toBeVisible()

      fireEvent.click(toggle)
      expect(toggle).toHaveAttribute('aria-expanded', 'false')
    })

    it('una sección sin sub-encabezados se muestra siempre desplegada, sin acordeón', () => {
      renderChapter('## Antes de comenzar\n\nDebes tener un potrero.')
      expect(screen.queryByRole('button', { name: /Antes de comenzar/ })).not.toBeInTheDocument()
      expect(screen.getByText('Debes tener un potrero.')).toBeVisible()
    })

    it('un hash de la URL que apunta a un sub-encabezado abre esa sección de entrada', () => {
      render(
        <MemoryRouter initialEntries={['/manual/sanidad#procedimiento']}>
          <ManualViewer chapter={chapter(contenidoConOperacion)} />
        </MemoryRouter>,
      )
      expect(screen.getByRole('button', { name: /Plan sanitario/ })).toHaveAttribute('aria-expanded', 'true')
    })
  })
})
