import { useState, useCallback } from 'react'
import TimePickerModal from './TimePickerModal'

const pad = (n) => String(n).padStart(2, '0')
const todayStr = () => {
  const d = new Date()
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`
}

const COLUMNS = [
  'FECHA',
  'CLIENTE',
  'PLACA',
  'CUSTODIOS',
  'INICIO VIAJE',
  'FINAL VIAJE',
  '#TELEVIA',
  '#PORTATIL',
  'SALDO PORTATIL',
  'VIATICO INICIAL',
  'VIATICO FINAL',
  'CASETAS',
  'TELEVIA',
  'PASE SALDO INICIAL',
  'PASES',
  'UVICUOS',
  'COMENTARIOS',
]

const timeColumns = new Set(['INICIO VIAJE', 'FINAL VIAJE'])

function emptyRow() {
  return COLUMNS.reduce((acc, col) => {
    acc[col] = col === 'FECHA' ? todayStr() : ''
    return acc
  }, {})
}

export default function Dashboard() {
  const [rows, setRows] = useState([emptyRow()])
  const [timePicker, setTimePicker] = useState(null) // { rowIndex, field }

  const updateCell = useCallback((rowIndex, field, value) => {
    setRows((prev) => {
      const next = prev.map((row, i) =>
        i === rowIndex ? { ...row, [field]: value } : row
      )
      return next
    })
  }, [])

  const addRow = () => setRows((prev) => [...prev, emptyRow()])
  const removeRow = () => setRows((prev) => (prev.length > 1 ? prev.slice(0, -1) : prev))
  const addRows = (n) => {
    const count = Math.min(25, Math.max(1, Number(n) || 1))
    setRows((prev) => [...prev, ...Array(count).fill(null).map(emptyRow)])
  }

  const handleSave = () => {
    const data = { rows, exportedAt: new Date().toISOString() }
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'custody_main.db'
    a.click()
    URL.revokeObjectURL(url)
  }

  const [addCount, setAddCount] = useState(1)
  const [suggestionVisible, setSuggestionVisible] = useState({}) // { "rowIdx-col": true }

  return (
    <div className="container captura-table" style={{ overflowX: 'auto' }}>
      <div style={{ display: 'flex', gap: '12px', marginBottom: '16px', flexWrap: 'wrap', alignItems: 'center' }}>
        <button type="button" className="thm-btn" onClick={addRow}>
          Añadir fila
        </button>
        <button type="button" className="thm-btn" onClick={removeRow}>
          Quitar fila
        </button>
        <span style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <input
            type="number"
            min={1}
            max={25}
            value={addCount}
            onChange={(e) => setAddCount(Number(e.target.value) || 1)}
            style={{ width: '56px', padding: '6px 8px', borderRadius: '6px', border: '1px solid #e6e6e6' }}
          />
          <button type="button" className="thm-btn" onClick={() => addRows(addCount)}>
            Añadir 1-25 filas
          </button>
        </span>
        <button type="button" className="thm-btn" onClick={handleSave}>
          Guardar (custody_main.db)
        </button>
      </div>

      <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px', minWidth: '1800px' }}>
        <thead>
          <tr style={{ background: 'var(--tanspot-black)', color: '#fff' }}>
            {COLUMNS.map((col) => (
              <th key={col} style={{ padding: '10px 8px', textAlign: 'left', whiteSpace: 'nowrap' }}>
                {col}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, rowIndex) => (
            <tr key={rowIndex}>
              {COLUMNS.map((col) => {
                const isFecha = col === 'FECHA'
                const isTime = timeColumns.has(col)
                const value = row[col] ?? ''
                const key = `${rowIndex}-${col}`
                const showNoSuggestions = suggestionVisible[key]

                return (
                  <td key={col} style={{ padding: '4px', verticalAlign: 'top' }}>
                    {isTime ? (
                      <input
                        type="text"
                        readOnly
                        value={value}
                        onClick={() => setTimePicker({ rowIndex, field: col, value })}
                        placeholder="dd/mm/yyyy HH:MM:SS"
                        className="captura-table-input"
                        style={{ width: '100%', minWidth: '140px', padding: '6px 8px', cursor: 'pointer' }}
                      />
                    ) : (
                      <>
                        <input
                          type="text"
                          readOnly={isFecha}
                          value={value}
                          onChange={(e) => updateCell(rowIndex, col, e.target.value)}
                          onFocus={() => setSuggestionVisible((s) => ({ ...s, [key]: true }))}
                          onBlur={() => setSuggestionVisible((s) => ({ ...s, [key]: false }))}
                          placeholder={isFecha ? '' : ''}
                          className="captura-table-input"
                          style={{ width: '100%', minWidth: '80px', padding: '6px 8px' }}
                        />
                        {showNoSuggestions && !isFecha && value !== '' && (
                          <div style={{ fontSize: '11px', color: 'var(--tanspot-gray)', marginTop: '2px' }}>No sugerencias</div>
                        )}
                      </>
                    )}
                  </td>
                )
              })}
            </tr>
          ))}
        </tbody>
      </table>

      {timePicker && (
        <TimePickerModal
          initialValue={timePicker.value}
          onConfirm={(formatted) => {
            updateCell(timePicker.rowIndex, timePicker.field, formatted)
          }}
          onClose={() => setTimePicker(null)}
        />
      )}
    </div>
  )
}
