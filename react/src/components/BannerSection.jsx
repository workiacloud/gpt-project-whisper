import { useState, useEffect, useRef } from 'react'
import Typed from 'typed.js'

export default function BannerSection({ onLogin }) {
  return (
    <section className="banner-one login-screen-banner" id="home">
      <div
        className="banner-one__pattern"
        style={{ backgroundImage: 'url(/assets/images/shapes/banner-ong-pattern.png)' }}
      />
      <div className="banner-one__img" data-aos="fade-left" data-aos-duration="1000" data-aos-delay="0">
        <img src="/assets/images/resources/banner-one-img-1.png" alt="" />
      </div>
      <div className="banner-one__shape-3 float-bob-y">
        <img src="/assets/images/shapes/banner-one-shape-3.png" alt="" style={{ width: '100%', height: '100%' }} />
      </div>
      <div className="banner-one__line-shpae1" />
      <div className="banner-one__line-shpae2" />
      <div className="container">
        <div className="banner-one__inner">
          <div className="banner-one__content-box login-center-wrapper">
            <div className="login-center-box">
              <EmployeeAccessContent onLogin={onLogin} />
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}

function EmployeeAccessContent({ onLogin }) {
  const [usuario, setUsuario] = useState('')
  const [password, setPassword] = useState('')
  const refTitle = useRef(null)
  const refSubtitle = useRef(null)
  const refHeading = useRef(null)
  const refLegend = useRef(null)

  useEffect(() => {
    const t1 = refTitle.current ? new Typed(refTitle.current, { strings: ['CUSTODY ERP'], typeSpeed: 80, showCursor: false }) : null
    const t2 = refSubtitle.current ? new Typed(refSubtitle.current, { strings: ['Sistema de custodia'], typeSpeed: 60, startDelay: 400, showCursor: false }) : null
    const t3 = refHeading.current ? new Typed(refHeading.current, { strings: ['Acceso Empleados'], typeSpeed: 60, startDelay: 900, showCursor: false }) : null
    const t4 = refLegend.current ? new Typed(refLegend.current, { strings: ['Ingrese sus credenciales para acceder al sistema.'], typeSpeed: 40, startDelay: 1400, showCursor: false }) : null
    return () => {
      t1?.destroy()
      t2?.destroy()
      t3?.destroy()
      t4?.destroy()
    }
  }, [])

  const handleSubmit = (e) => {
    e.preventDefault()
    onLogin()
  }

  return (
    <div className="banner-login-content">
      <div className="logo" style={{ marginBottom: '24px' }}>
        <span ref={refTitle} style={{ fontSize: '56px', fontWeight: 700, color: 'var(--tanspot-base)', minHeight: '1.2em', display: 'inline-block' }} />
        <p style={{ fontSize: '24px', color: 'rgba(255,255,255,0.9)', marginTop: '4px', textTransform: 'uppercase', minHeight: '1.2em' }}>
          <span ref={refSubtitle} />
        </p>
      </div>
      <div className="content-box" style={{ marginBottom: '24px' }}>
        <h4 ref={refHeading} style={{ color: '#fff', marginBottom: '12px', minHeight: '1.3em' }} />
        <p style={{ color: 'rgba(255,255,255,0.85)', fontSize: '14px', minHeight: '2.5em' }}>
          <span ref={refLegend} />
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
              style={{
                width: '100%',
                padding: '12px 16px',
                borderRadius: '8px',
                border: '1px solid rgba(255,255,255,0.2)',
                background: 'rgba(0,0,0,0.2)',
                color: '#fff',
              }}
            />
          </div>
          <div className="form-group">
            <input
              type="password"
              placeholder="Contraseña"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              style={{
                width: '100%',
                padding: '12px 16px',
                borderRadius: '8px',
                border: '1px solid rgba(255,255,255,0.2)',
                background: 'rgba(0,0,0,0.2)',
                color: '#fff',
              }}
            />
          </div>
          <div className="form-group message-btn" style={{ marginTop: '24px', textAlign: 'center' }}>
            <button type="submit" className="thm-btn">
              Iniciar sesión
              <span><i className="fas fa-arrow-right" /></span>
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
