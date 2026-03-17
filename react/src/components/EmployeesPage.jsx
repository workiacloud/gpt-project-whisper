import { useCallback, useEffect, useMemo, useState } from 'react'
import EmployeesTable from './EmployeesTable'
import EmployeesPagination from './EmployeesPagination'
import { getEmployeesPage } from '../services/employeeApi'

export default function EmployeesPage({ isNightMode }) {
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [sortBy, setSortBy] = useState('id')
  const [sortDir, setSortDir] = useState('asc')
  const [data, setData] = useState({
    items: [],
    page: 0,
    size: 10,
    totalItems: 0,
    totalPages: 0,
    hasNext: false,
    hasPrevious: false
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const loadPage = useCallback(async () => {
    setLoading(true)
    setError('')

    try {
      const response = await getEmployeesPage(page, size, sortBy, sortDir)
      setData(response)
    } catch (err) {
      setError(err.message || 'Failed to load employees')
    } finally {
      setLoading(false)
    }
  }, [page, size, sortBy, sortDir])

  useEffect(() => {
    loadPage()
  }, [loadPage])

  const columns = useMemo(() => {
    const first = data.items?.[0]
    const keys = first?.data ? Object.keys(first.data) : []
    return ['id', ...keys, 'version']
  }, [data.items])

  const handleSort = (column) => {
    setPage(0)
    if (column === sortBy) {
      setSortDir((current) => (current === 'asc' ? 'desc' : 'asc'))
      return
    }
    setSortBy(column)
    setSortDir('asc')
  }

  return (
    <section className="employees-module">
      <div className="container">
        <div className={`employees-card ${isNightMode ? 'night' : ''}`}>
          <div className="employees-toolbar">
            <div>
              <h3 className="employees-title">Employee</h3>
              <p className="employees-subtitle">
                Datos resueltos desde cache Valkey.
              </p>
            </div>

            <div className="employees-size-box">
              <label htmlFor="employees-page-size">Filas por página</label>
              <select
                id="employees-page-size"
                value={size}
                onChange={(e) => {
                  setPage(0)
                  setSize(Number(e.target.value))
                }}
              >
                <option value={10}>10</option>
                <option value={20}>20</option>
                <option value={50}>50</option>
              </select>
            </div>
          </div>

          {error && <div className="employees-alert-error">{error}</div>}

          <EmployeesTable
            columns={columns}
            rows={data.items}
            loading={loading}
            sortBy={sortBy}
            sortDir={sortDir}
            onSort={handleSort}
          />

          <EmployeesPagination
            page={data.page}
            totalPages={data.totalPages}
            totalItems={data.totalItems}
            hasNext={data.hasNext}
            hasPrevious={data.hasPrevious}
            onPrevious={() => setPage((p) => Math.max(0, p - 1))}
            onNext={() => setPage((p) => p + 1)}
            onGoToPage={(nextPage) => setPage(nextPage)}
          />
        </div>
      </div>
    </section>
  )
}