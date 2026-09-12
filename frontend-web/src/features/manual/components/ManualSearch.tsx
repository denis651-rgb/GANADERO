import { useDeferredValue, useState } from 'react'
import { useNavigate } from 'react-router'
import { Search } from 'lucide-react'
import { searchManual } from '@/features/manual/search'
import { ManualHighlight } from '@/features/manual/components/ManualHighlight'
import { Field } from '@/shared/components/Field'

export function ManualSearch() {
  const navigate = useNavigate()
  const [query, setQuery] = useState('')
  const [open, setOpen] = useState(false)
  const deferredQuery = useDeferredValue(query)
  const trimmed = deferredQuery.trim()
  const results = trimmed.length >= 2 ? searchManual(trimmed) : []

  function goTo(chapterId: string, anchor: string) {
    navigate(`/manual/${chapterId}${anchor ? `#${anchor}` : ''}`)
    setQuery('')
    setOpen(false)
  }

  return (
    <div className="picker-root manual-search">
      <Field label="Buscar en el manual" icon={<Search size={18} aria-hidden="true" />}>
        <input
          type="search"
          value={query}
          onChange={(event) => { setQuery(event.target.value); setOpen(true) }}
          onFocus={() => setOpen(true)}
          onBlur={() => setOpen(false)}
          placeholder="Ej. edad aproximada, crear plan, registrar parto…"
        />
      </Field>
      {open && trimmed.length >= 2 && (
        <div className="picker-results manual-search-results">
          {results.length === 0 && <div className="picker-empty">Sin resultados para «{trimmed}».</div>}
          {results.map((result, index) => (
            <button
              key={`${result.chapterId}-${result.anchor}-${index}`}
              type="button"
              className="picker-option manual-search-option"
              onMouseDown={(event) => { event.preventDefault(); goTo(result.chapterId, result.anchor) }}
            >
              <span className="manual-search-chapter">{result.chapterTitle}</span>
              <span className="manual-search-heading"><ManualHighlight text={result.heading} query={trimmed} /></span>
              {result.excerpt && (
                <span className="manual-search-excerpt"><ManualHighlight text={result.excerpt} query={trimmed} /></span>
              )}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
