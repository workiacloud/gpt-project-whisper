import { useState } from 'react'

export default function EmployeeAccess({ onLogin, variant }) {
  const [usuario, setUsuario] = useState('')
  const [password, setPassword] = useState('')

  const handleSubmit = (e) => {
    e.preventDefault()
    onLogin()
  }

  const formContent = (
    <div className="content-inner">
      <div className="logo" style={{ marginBottom: '24px' }}>
        <span style={{ fontSize: '28px', fontWeight: 700, color: 'var(--tanspot-base)' }}>CUSTODY ERP</span>
        <p style={{ fontSize: '12px', color: '#fff', marginTop: '4px', textTransform: 'uppercase' }}>Sistema de custodia</p>
      </div>
      <div className="content-box" style={{ marginBottom: '24px' }}>
        <h4 style={{ color: '#fff', marginBottom: '12px' }}>Acceso Empleados</h4>
        <p style={{ color: 'rgba(255,255,255,0.85)', fontSize: '14px' }}>
          Ingrese sus credenciales para acceder al sistema.
        </p>
      </div>
      <div className="form-inner">
        <form onSubmit={handleSubmit} className="contact-form-validated">
          <div className="form-group">
            <input
              type="text"
              placeholder="Usuario"
              value={usuario}
              onChange={(e) => setUsuario(e.target.value)}
              required
              style={{ width: '100%', padding: '12px 16px', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.2)', background: 'rgba(0,0,0,0.2)', color: '#fff' }}
            />
          </div>
          <div className="form-group">
            <input
              type="password"
              placeholder="Contraseña"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              style={{ width: '100%', padding: '12px 16px', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.2)', background: 'rgba(0,0,0,0.2)', color: '#fff' }}
            />
          </div>
          <div className="form-group message-btn">
            <button type="submit" className="thm-btn">
              Iniciar sesión
              <span><i className="fas fa-arrow-right" /></span>
            </button>
          </div>
        </form>
      </div>
    </div>
  )

  if (variant === 'panel') {
    return (
      <div className="employee-access-panel">
        <div className="sidebar-textwidget">
          <div className="sidebar-info-contents">{formContent}</div>
        </div>
      </div>
    )
  }

  return (
    <div className="xs-sidebar-group info-group info-sidebar isActive" style={{ display: 'block' }}>
      <div className="xs-overlay xs-bg-black" style={{ opacity: 0.8, visibility: 'visible' }} />
      <div className="xs-sidebar-widget" style={{ opacity: 1, visibility: 'visible', transform: 'translateX(0)' }}>
        <div className="sidebar-widget-container" style={{ transform: 'translateX(0)', visibility: 'visible' }}>
          <div className="sidebar-textwidget">
            <div className="sidebar-info-contents">{formContent}</div>
          </div>
        </div>
      </div>
    </div>
  )
}
