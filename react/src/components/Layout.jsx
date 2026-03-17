import { useEffect, useMemo, useRef, useState } from 'react'
import Typed from 'typed.js'
import Dashboard from './Dashboard'
import EmployeesPage from './EmployeesPage'

export default function Layout({ isNightMode, onNightModeToggle, onLogout }) {
  const [currentPage, setCurrentPage] = useState('employees')
  const typedElementRef = useRef(null)

  const menuItems = useMemo(
    () => [
      { key: 'dashboard', label: 'Dashboard' },
      { key: 'captura', label: 'Captura' },
      { key: 'catalogos', label: 'Catalogos' },
      {
        key: 'recursos-humanos',
        label: 'Recursos Humanos',
        children: [
          { key: 'employees', label: 'Empleados' }
        ]
      },
      { key: 'conciliacion', label: 'Conciliacion' },
      {
        key: 'sistema',
        label: 'Sistema',
        children: [
          { key: 'usuarios', label: 'Usuarios' },
          { key: 'roles', label: 'Roles' }
        ]
      }
    ],
    []
  )

  const pageLegends = useMemo(
    () => ({
      dashboard: ['Panel de control...'],
      employees: ['Empleados registrados en cache Valkey con paginación.'],
      catalogos: ['Catálogos y referencias...'],
      conciliacion: ['Conciliación...'],
      usuarios: ['Usuarios del sistema...'],
      roles: ['Roles y permisos...']
    }),
    []
  )

  useEffect(() => {
    if (!typedElementRef.current || currentPage === 'captura') {
      return
    }

    const typed = new Typed(typedElementRef.current, {
      strings: pageLegends[currentPage] || ['Contenido próximamente...'],
      typeSpeed: 35,
      backSpeed: 20,
      showCursor: false
    })

    return () => typed.destroy()
  }, [currentPage, pageLegends])

  const renderPage = () => {
    switch (currentPage) {
      case 'dashboard':
        return <Dashboard />
      case 'employees':
        return <EmployeesPage isNightMode={isNightMode} />
      default:
        return (
          <div className="container" style={{ padding: '32px 0 48px' }}>
            <p ref={typedElementRef} />
          </div>
        )
    }
  }

  const titleMap = {
    dashboard: 'Dashboard',
    employees: 'Empleados',
    catalogos: 'Catalogos',
    'recursos-humanos': 'Recursos Humanos',
    conciliacion: 'Conciliacion',
    usuarios: 'Usuarios',
    roles: 'Roles'
  }

  return (
    <div className="page-wrapper">
      <header className="main-header clearfix">
        <nav className="main-menu">
          <div className="main-menu__wrapper">
            <div className="main-menu__wrapper-inner">
              <div className="main-menu__left">
                <div className="main-menu__logo">
                  <a href="#" onClick={(e) => e.preventDefault()}>
                    CUSTODY ERP
                  </a>
                </div>
              </div>

              <div className="main-menu__main-menu-box">
                <ul className="main-menu__list">
                  {menuItems.map((item) =>
                    item.children ? (
                      <li key={item.key} className="dropdown">
                        <a href="#" onClick={(e) => e.preventDefault()}>
                          {item.label}
                        </a>
                        <ul>
                          {item.children.map((child) => (
                            <li key={child.key}>
                              <a
                                href="#"
                                onClick={(e) => {
                                  e.preventDefault()
                                  setCurrentPage(child.key)
                                }}
                              >
                                {child.label}
                              </a>
                            </li>
                          ))}
                        </ul>
                      </li>
                    ) : (
                      <li key={item.key}>
                        <a
                          href="#"
                          onClick={(e) => {
                            e.preventDefault()
                            setCurrentPage(item.key)
                          }}
                        >
                          {item.label}
                        </a>
                      </li>
                    )
                  )}
                </ul>
              </div>

              <div className="main-menu__right">
                <div className="main-menu__night-mode-switch">
                  <label
                    className="night-mode-toggle"
                    aria-label={isNightMode ? 'Cambiar a modo día' : 'Cambiar a modo noche'}
                  >
                    <input
                      type="checkbox"
                      checked={isNightMode}
                      onChange={onNightModeToggle}
                    />
                    <span className="night-mode-slider">
                      <span className="night-mode-icon">{isNightMode ? '☀' : '☽'}</span>
                    </span>
                  </label>
                </div>

                <button className="thm-btn" type="button" onClick={onLogout}>
                  Salir
                </button>
              </div>
            </div>
          </div>
        </nav>
      </header>

      <main>
        <section className="page-header" style={{ padding: '36px 0 12px' }}>
          <div className="container">
            <h2 style={{ marginBottom: 8 }}>{titleMap[currentPage] || 'Modulo'}</h2>
            <p ref={typedElementRef} style={{ margin: 0 }} />
          </div>
        </section>

        {renderPage()}
      </main>
    </div>
  )
}