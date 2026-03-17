export default function EmployeesTable({
  columns,
  rows,
  loading,
  sortBy,
  sortDir,
  onSort
}) {
  const renderCell = (row, column) => {
    if (column === 'id') {
      return row.id
    }

    if (column === 'version') {
      return row.version
    }

    const value = row.data?.[column]

    if (value === null || value === undefined) {
      return ''
    }

    if (typeof value === 'object') {
      return JSON.stringify(value)
    }

    return String(value)
  }

  const renderSortIndicator = (column) => {
    if (column !== sortBy) {
      return '↕'
    }
    return sortDir === 'asc' ? '↑' : '↓'
  }

  return (
    <div className="employees-table captura-table">
      <table>
        <thead>
          <tr>
            {columns.map((column) => (
              <th key={column}>
                <button
                  type="button"
                  className="employees-sort-button"
                  onClick={() => onSort(column)}
                >
                  <span>{column}</span>
                  <span className="employees-sort-indicator">
                    {renderSortIndicator(column)}
                  </span>
                </button>
              </th>
            ))}
          </tr>
        </thead>

        <tbody>
          {loading ? (
            <tr>
              <td colSpan={columns.length || 1} className="employees-empty-cell">
                Loading...
              </td>
            </tr>
          ) : rows.length === 0 ? (
            <tr>
              <td colSpan={columns.length || 1} className="employees-empty-cell">
                No employees found
              </td>
            </tr>
          ) : (
            rows.map((row) => (
              <tr key={row.id}>
                {columns.map((column) => (
                  <td key={`${row.id}-${column}`}>{renderCell(row, column)}</td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  )
}