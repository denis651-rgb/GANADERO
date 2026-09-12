import { Fragment } from 'react'
import { foldText } from '@/features/manual/search'

/** Resalta la primera coincidencia de `query` dentro de `text`, ignorando mayúsculas/tildes. */
export function ManualHighlight({ text, query }: { text: string; query: string }) {
  const trimmedQuery = query.trim()
  if (!trimmedQuery) return <Fragment>{text}</Fragment>
  const index = foldText(text).indexOf(foldText(trimmedQuery))
  if (index === -1) return <Fragment>{text}</Fragment>
  const before = text.slice(0, index)
  const match = text.slice(index, index + trimmedQuery.length)
  const after = text.slice(index + trimmedQuery.length)
  return <Fragment>{before}<mark>{match}</mark>{after}</Fragment>
}
