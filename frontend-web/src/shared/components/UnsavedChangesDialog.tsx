import { ConfirmDialog } from '@/shared/components/ConfirmDialog'

export function UnsavedChangesDialog({ open, onStay, onLeave }: { open: boolean; onStay: () => void; onLeave: () => void }) {
  return (
    <ConfirmDialog
      open={open}
      title="Tienes cambios sin guardar"
      confirmLabel="Salir sin guardar"
      cancelLabel="Seguir editando"
      variant="danger"
      onClose={onStay}
      onConfirm={onLeave}
    >
      <p className="muted">Si sales ahora, perderás los cambios realizados.</p>
    </ConfirmDialog>
  )
}
