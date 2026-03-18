import { API_BASE_URL } from '../config/api'

const EMPLOYEE_BASE_URL = `${API_BASE_URL}/tables/employee`

async function parseJson(response, message) {
  if (!response.ok) {
    throw new Error(`${message} (status ${response.status})`)
  }

  return response.json()
}

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

  const response = await fetch(`${EMPLOYEE_BASE_URL}?${params.toString()}`)
  return parseJson(response, 'Employee request failed')
}

export async function getEmployeeMeta() {
  const response = await fetch(`${EMPLOYEE_BASE_URL}/meta`)
  return parseJson(response, 'Employee meta request failed')
}

export async function getEmployeeAutocomplete(field, q, limit = 8) {
  if (!q || !q.trim()) {
    return []
  }

  const params = new URLSearchParams({
    field,
    q,
    limit: String(limit)
  })

  const response = await fetch(`${EMPLOYEE_BASE_URL}/autocomplete?${params.toString()}`)
  return parseJson(response, 'Employee autocomplete failed')
}

export async function searchEmployees(field, value) {
  const params = new URLSearchParams({
    field,
    value
  })

  const response = await fetch(`${EMPLOYEE_BASE_URL}/search?${params.toString()}`)
  return parseJson(response, 'Employee search failed')
}

export async function updateEmployeeRow({ id, expectedVersion, data, whoId }) {
  const payload = {
    id,
    expectedVersion,
    data
  }

  if (whoId != null) {
    payload.whoId = whoId
  }

  const response = await fetch(`${EMPLOYEE_BASE_URL}/update`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload)
  })

  return parseJson(response, 'Employee update failed')
}

export async function insertEmployeeRow({ data, whoId }) {
  const payload = { data }

  if (whoId != null) {
    payload.whoId = whoId
  }

  const response = await fetch(`${EMPLOYEE_BASE_URL}/insert`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload)
  })

  return parseJson(response, 'Employee insert failed')
}