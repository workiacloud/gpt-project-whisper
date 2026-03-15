import { useState } from 'react'

const pad = (n) => String(n).padStart(2, '0')

export default function TimePickerModal({ initialValue = '', onConfirm, onClose }) {
  const parseValue = (v) => {
    if (!v) {
      const d = new Date()
      return {
        day: pad(d.getDate()),
        month: pad(d.getMonth() + 1),
        year: d.getFullYear(),
        hour: pad(d.getHours()),
        min: pad(d.getMinutes()),
        sec: pad(d.getSeconds()),
      }
    }
    const [datePart, timePart] = v.split(' ')
    const [d, m, y] = (datePart || '').split('/')
    const [h, min, s] = (timePart || '').split(':')
    return {
      day: d || pad(new Date().getDate()),
      month: m || pad(new Date().getMonth() + 1),
      year: y || String(new Date().getFullYear()),
      hour: h || '00',
      min: min || '00',
      sec: s || '00',
    }
  }
  const [state, setState] = useState(parseValue(initialValue))

  const update = (key, value) => setState((s) => ({ ...s, [key]: value }))

  const formatOutput = () =>
    `${state.day}/${state.month}/${state.year} ${state.hour}:${state.min}:${state.sec}`

  const handleConfirm = () => {
    onConfirm(formatOutput())
    onClose()
  }

  return (
    <div
      className="time-picker-overlay"
      onClick={onClose}
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(0,0,0,0.5)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 10000,
      }}
    >
      <div
        className="time-picker-modal"
        onClick={(e) => e.stopPropagation()}
        style={{
          background: '#fff',
          padding: '24px',
          borderRadius: '10px',
          minWidth: '320px',
          boxShadow: '0 4px 20px rgba(0,0,0,0.2)',
        }}
      >
        <h4 style={{ marginBottom: '16px', color: 'var(--tanspot-black)' }}>Fecha y hora (dd/mm/yyyy HH:MM:SS)</h4>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '8px', marginBottom: '12px' }}>
          <input
            type="text"
            placeholder="DD"
            value={state.day}
            onChange={(e) => update('day', e.target.value)}
            maxLength={2}
            style={{ padding: '8px', borderRadius: '6px', border: '1px solid #e6e6e6' }}
          />
          <input
            type="text"
            placeholder="MM"
            value={state.month}
            onChange={(e) => update('month', e.target.value)}
            maxLength={2}
            style={{ padding: '8px', borderRadius: '6px', border: '1px solid #e6e6e6' }}
          />
          <input
            type="text"
            placeholder="YYYY"
            value={state.year}
            onChange={(e) => update('year', e.target.value)}
            maxLength={4}
            style={{ padding: '8px', borderRadius: '6px', border: '1px solid #e6e6e6' }}
          />
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '8px', marginBottom: '16px' }}>
          <input
            type="text"
            placeholder="HH"
            value={state.hour}
            onChange={(e) => update('hour', e.target.value)}
            maxLength={2}
            style={{ padding: '8px', borderRadius: '6px', border: '1px solid #e6e6e6' }}
          />
          <input
            type="text"
            placeholder="MM"
            value={state.min}
            onChange={(e) => update('min', e.target.value)}
            maxLength={2}
            style={{ padding: '8px', borderRadius: '6px', border: '1px solid #e6e6e6' }}
          />
          <input
            type="text"
            placeholder="SS"
            value={state.sec}
            onChange={(e) => update('sec', e.target.value)}
            maxLength={2}
            style={{ padding: '8px', borderRadius: '6px', border: '1px solid #e6e6e6' }}
          />
        </div>
        <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
          <button type="button" className="thm-btn" onClick={onClose} style={{ padding: '8px 16px' }}>
            Cancelar
          </button>
          <button type="button" className="thm-btn" onClick={handleConfirm} style={{ padding: '8px 16px' }}>
            Aceptar
          </button>
        </div>
      </div>
    </div>
  )
}
