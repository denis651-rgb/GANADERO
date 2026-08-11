import { useCallback, useEffect, useRef, useState } from 'react'

export function useUnsavedChanges(isDirty: boolean) {
  const pendingAction = useRef<(() => void) | null>(null)
  const [open, setOpen] = useState(false)

  useEffect(() => {
    if (!isDirty) return
    const beforeUnload = (event: BeforeUnloadEvent) => event.preventDefault()
    window.addEventListener('beforeunload', beforeUnload)
    return () => window.removeEventListener('beforeunload', beforeUnload)
  }, [isDirty])

  const requestLeave = useCallback((action: () => void) => {
    if (!isDirty) {
      action()
      return
    }
    pendingAction.current = action
    setOpen(true)
  }, [isDirty])

  const cancelLeave = useCallback(() => {
    pendingAction.current = null
    setOpen(false)
  }, [])

  const discardAndLeave = useCallback(() => {
    const action = pendingAction.current
    pendingAction.current = null
    setOpen(false)
    action?.()
  }, [])

  return { open, requestLeave, cancelLeave, discardAndLeave }
}
