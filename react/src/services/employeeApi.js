import { API_BASE_URL } from '../config/api'

export async function getEmployeesPage(
  page = 0,
  size = 10,
  sortBy = 'id',
  sortDir = 'asc'
) {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
    sortBy,
    sortDir
  })

  const response = await fetch(
    `${API_BASE_URL}/tables/employee?${params.toString()}`
  )

  if (!response.ok) {
    throw new Error(`Employee request failed with status ${response.status}`)
  }

  return response.json()
}