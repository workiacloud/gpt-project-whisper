import { useEffect, useState } from 'react'
import EmployeeAccess from './components/EmployeeAccess'
import BannerSection from './components/BannerSection'
import Layout from './components/Layout'
import shapeBg from './assets/images/shapes/download-app-one-shpae-bg.png'
import shapeBgTwo from './assets/images/shapes/download-app-one-shpae-bg-two.png'

function App() {
  const [isLoggedIn, setIsLoggedIn] = useState(false)
  const [isNightMode, setIsNightMode] = useState(false)

  useEffect(() => {
    const cursor = document.querySelector('.custom-cursor__cursor')
    const cursorTwo = document.querySelector('.custom-cursor__cursor-two')

    if (!cursor || !cursorTwo) {
      return
    }

    const onMove = (e) => {
      cursor.style.transform = `translate3d(${e.clientX - 12}px, ${e.clientY - 12}px, 0)`
      cursorTwo.style.transform = `translate3d(${e.clientX - 5}px, ${e.clientY - 5}px, 0)`
    }

    window.addEventListener('mousemove', onMove)
    return () => window.removeEventListener('mousemove', onMove)
  }, [])

  return (
    <div className={`app-page-background ${isNightMode ? 'night-mode' : ''}`}>
      <div
        className="download-app-one__shape-bg"
        style={{ backgroundImage: `url(${shapeBg})` }}
      />
      <div
        className="download-app-one__shape-bg-two"
        style={{ backgroundImage: `url(${shapeBgTwo})` }}
      />

      <div className="app-page-content">
        {!isLoggedIn ? (
          <div className="login-screen-layout">
            <div className="login-screen-panel">
              <EmployeeAccess onLogin={() => setIsLoggedIn(true)} />
            </div>

            <div className="login-screen-banner-wrap">
              <BannerSection className="login-screen-banner" />
            </div>
          </div>
        ) : (
          <Layout
            isNightMode={isNightMode}
            onNightModeToggle={() => setIsNightMode((v) => !v)}
            onLogout={() => setIsLoggedIn(false)}
          />
        )}
      </div>
    </div>
  )
}

export default App