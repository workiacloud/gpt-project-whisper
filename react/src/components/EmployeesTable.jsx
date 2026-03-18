import { useEffect, useRef, useState } from 'react'

export default function EmployeesTable({
  columns,
  columnLabels,
  rows,
  loading,
  sortBy,
  sortDir,
  onSort,
  onSaveCell,
  onAutocomplete
}) {
  const [drafts, setDrafts] = useState({})
  const [saving, setSaving] = useState({})
  const [errors, setErrors] = useState({})
  const [suggestions, setSuggestions] = useState({})
  const [openSuggestionKey, setOpenSuggestionKey] = useState(null)
  const debounceTimersRef = useRef({})

  useEffect(() => {
    const nextDrafts = {}

    for (const row of rows) {
      for (const column of columns) {
        nextDrafts[cellKey(row.id, column)] = row.data?.[column] ?? ''
      }
    }

    setDrafts(nextDrafts)
  }, [rows, columns])

  useEffect(() => {
    return () => {
      Object.values(debounceTimersRef.current).forEach((timerId) => {
        clearTimeout(timerId)
      })
    }
  }, [])

  const renderSortIndicator = (column) => {
    if (column !== sortBy) {
      return '↕'
    }
    return sortDir === 'asc' ? '↑' : '↓'
  }

  const handleChange = (row, column, value) => {
    const key = cellKey(row.id, column)

    setDrafts((prev) => ({
      ...prev,
      [key]: value
    }))

    setErrors((prev) => ({
      ...prev,
      [key]: ''
    }))

    if (debounceTimersRef.current[key]) {
      clearTimeout(debounceTimersRef.current[key])
    }

    debounceTimersRef.current[key] = setTimeout(async () => {
      try {
        const items = await onAutocomplete(column, value)
        setSuggestions((prev) => ({
          ...prev,
          [key]: items || []
        }))
      } catch {
        setSuggestions((prev) => ({
          ...prev,
          [key]: []
        }))
      }
    }, 180)
  }

  const handleBlur = async (row, column) => {
    const key = cellKey(row.id, column)
    const originalValue = row.data?.[column] ?? ''
    const nextValue = drafts[key] ?? ''

    window.setTimeout(async () => {
      if (openSuggestionKey === key) {
        return
      }

      setSuggestions((prev) => ({
        ...prev,
        [key]: []
      }))

      if (String(originalValue) === String(nextValue)) {
        return
      }

      setSaving((prev) => ({
        ...prev,
        [key]: true
      }))

      setErrors((prev) => ({
        ...prev,
        [key]: ''
      }))

      try {
        await onSaveCell({
          rowId: row.id,
          rowVersion: row.version,
          field: column,
          value: nextValue
        })
      } catch (error) {
        setDrafts((prev) => ({
          ...prev,
          [key]: originalValue
        }))

        setErrors((prev) => ({
          ...prev,
          [key]: error.message || 'No se pudo guardar'
        }))
      } finally {
        setSaving((prev) => ({
          ...prev,
          [key]: false
        }))
      }
    }, 120)
  }

  const handleSuggestionMouseDown = async (event, row, column, suggestion) => {
    event.preventDefault()

    const key = cellKey(row.id, column)

    setOpenSuggestionKey(key)

    setDrafts((prev) => ({
      ...prev,
      [key]: suggestion
    }))

    setSuggestions((prev) => ({
      ...prev,
      [key]: []
    }))

    setSaving((prev) => ({
      ...prev,
      [key]: true
    }))

    setErrors((prev) => ({
      ...prev,
      [key]: ''
    }))

    try {
      await onSaveCell({
        rowId: row.id,
        rowVersion: row.version,
        field: column,
        value: suggestion
      })
    } catch (error) {
      setDrafts((prev) => ({
        ...prev,
        [key]: row.data?.[column] ?? ''
      }))

      setErrors((prev) => ({
        ...prev,
        [key]: error.message || 'No se pudo guardar'
      }))
    } finally {
      setSaving((prev) => ({
        ...prev,
        [key]: false
      }))
      setOpenSuggestionKey(null)
    }
  }

  return (
    <div className="employees-table captura-table">
      <table>
        <thead>
          <tr>
            {columns.map((column) => (
              <th key={column}>
                <button
                  type="button"
                  className="employees-sort-button"
                  onClick={() => onSort(column)}
                >
                  <span>{columnLabels?.[column] || column}</span>
                  <span className="employees-sort-indicator">
                    {renderSortIndicator(column)}
                  </span>
                </button>
              </th>
            ))}
          </tr>
        </thead>

        <tbody>
          {loading ? (
            <tr>
              <td colSpan={columns.length || 1} className="employees-empty-cell">
                Loading...
              </td>
            </tr>
          ) : rows.length === 0 ? (
            <tr>
              <td colSpan={columns.length || 1} className="employees-empty-cell">
                No employees found
              </td>
            </tr>
          ) : (
            rows.map((row) => (
              <tr key={row.id}>
                {columns.map((column) => {
                  const key = cellKey(row.id, column)
                  const value = drafts[key] ?? row.data?.[column] ?? ''
                  const cellSuggestions = suggestions[key] || []
                  const isSaving = Boolean(saving[key])
                  const error = errors[key]

                  return (
                    <td key={key}>
                      <div className="employees-editable-cell">
                        <input
                          type="text"
                          value={value}
                          onFocus={() => setOpenSuggestionKey(key)}
                          onChange={(e) => handleChange(row, column, e.target.value)}
                          onBlur={() => handleBlur(row, column)}
                        />

                        {isSaving && (
                          <div className="employees-cell-status">Guardando...</div>
                        )}

                        {!isSaving && error && (
                          <div className="employees-cell-error">{error}</div>
                        )}

                        {openSuggestionKey === key && cellSuggestions.length > 0 && (
                          <div className="employees-suggestions">
                            {cellSuggestions.map((suggestion) => (
                              <button
                                key={`${key}-${suggestion}`}
                                type="button"
                                className="employees-suggestion-item"
                                onMouseDown={(event) =>
                                  handleSuggestionMouseDown(event, row, column, suggestion)
                                }
                              >
                                {suggestion}
                              </button>
                            ))}
                          </div>
                        )}
                      </div>
                    </td>
                  )
                })}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  )
}

function cellKey(rowId, column) {
  return `${rowId}:${column}`
}