import { useState, useEffect, useRef } from 'react'
import Typed from 'typed.js';
import Dashboard from './Dashboard'

export default function Layout({ isNightMode, onNightModeToggle }) {
  const [currentPage, setCurrentPage] = useState('dashboard')
  const el = useRef(null);

  const menuItems = [
    { key: 'dashboard', label: 'Dashboard' },
    { key: 'captura', label: 'Captura' },
    { key: 'catalogos', label: 'Catalogos' },
    { key: 'recursos-humanos', label: 'Recursos Humanos' },
    { key: 'conciliacion', label: 'Conciliacion' },
    {
      key: 'sistema',
      label: 'Sistema',
      children: [
        { key: 'usuarios', label: 'Usuarios' },
        { key: 'roles', label: 'Roles' },
      ],
    },
  ]
  const pageLegends = {
    dashboard: ['Panel de control...'],
    catalogos: ['Catálogos y referencias...'],
    'recursos-humanos': ['Recursos Humanos...'],
    conciliacion: ['Conciliación...'],
    usuarios: ['Usuarios del sistema...'],
    roles: ['Roles y permisos...'],
  }

  useEffect(() => {
    if (currentPage === 'captura' || !el.current) return
    const typed = new Typed(el.current, {
      strings: pageLegends[currentPage] || ['Contenido próximamente...'],
      typeSpeed: 50,
    })
    return () => typed.destroy()
  }, [currentPage])

  return (
    <div className="page-wrapper">
      <header className="main-header">
        <nav className="main-menu">
          <div className="main-menu__wrapper">
            <div className="main-menu__wrapper-inner">
              <div className="main-menu__left">
                <div className="main-menu__logo">
                  <a href="#"><span style={{ fontSize: '20px', fontWeight: 700, color: '#fff' }}>CUSTODY ERP</span></a>
                </div>
              </div>
              <div className="main-menu__main-menu-box">
                <ul className="main-menu__list">
                  {menuItems.map((item) =>
                    item.children ? (
                      <li key={item.key} className="dropdown">
                        <a href="#">{item.label}</a>
                        <ul className="shadow-box">
                          {item.children.map((child) => (
                            <li key={child.key}>
                              <a href="#" onClick={(e) => { e.preventDefault(); setCurrentPage(child.key); }}>{child.label}</a>
                            </li>
                          ))}
                        </ul>
                      </li>
                    ) : (
                      <li key={item.key} className={currentPage === item.key ? 'current' : ''}>
                        <a href="#" onClick={(e) => { e.preventDefault(); setCurrentPage(item.key); }}>{item.label}</a>
                      </li>
                    )
                  )}
                </ul>
              </div>
              <div className="main-menu__right">
                <div className="main-menu__night-mode-switch">
                  <label className="night-mode-toggle" title={isNightMode ? 'Modo día' : 'Modo noche'}>
                    <input
                      type="checkbox"
                      checked={isNightMode ?? false}
                      onChange={onNightModeToggle ?? (() => {})}
                      aria-label={isNightMode ? 'Cambiar a modo día' : 'Cambiar a modo noche'}
                    />
                    <span className="night-mode-slider">
                      <span className="night-mode-icon" aria-hidden>{isNightMode ? '☀' : '☽'}</span>
                    </span>
                  </label>
                </div>
                <div className="main-menu__btn-box">
                  <a href="#" className="thm-btn">Salir<span><i className="fas fa-arrow-right" /></span></a>
                </div>
              </div>
            </div>
          </div>
        </nav>
      </header>

      <main style={{ paddingTop: '20px', minHeight: '80vh' }}>
        {currentPage === 'captura' && <Dashboard />}
        {currentPage !== 'captura' && (
          <div className="container">
            <p style={{ padding: '2rem', color: 'var(--tanspot-gray)' }}>
              {currentPage === 'dashboard' && 'Dashboard'}
              {currentPage === 'catalogos' && 'Catalogos'}
              {currentPage === 'recursos-humanos' && 'Recursos Humanos'}
              {currentPage === 'conciliacion' && 'Conciliacion'}
              {currentPage === 'usuarios' && 'Usuarios'}
              {currentPage === 'roles' && 'Roles'}
              {' — '}<span ref={el}></span>
            </p>
          </div>
        )}
      </main>
    </div>
  )
}
