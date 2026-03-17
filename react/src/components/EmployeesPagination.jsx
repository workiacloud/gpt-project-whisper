export default function EmployeesPagination({
  page,
  totalPages,
  totalItems,
  hasNext,
  hasPrevious,
  onPrevious,
  onNext,
  onGoToPage
}) {
  if (totalPages <= 1) {
    return (
      <div className="employees-pagination employees-pagination--single">
        <span>Total registros: {totalItems}</span>
      </div>
    )
  }

  const pages = []
  for (let i = 0; i < totalPages; i += 1) {
    pages.push(i)
  }

  return (
    <div className="employees-pagination">
      <div className="employees-pagination__summary">
        <span>Total registros: {totalItems}</span>
        <span>Página {page + 1} de {totalPages}</span>
      </div>

      <div className="employees-pagination__actions">
        <button type="button" onClick={onPrevious} disabled={!hasPrevious}>
          Anterior
        </button>

        <div className="employees-pagination__pages">
          {pages.map((pageIndex) => (
            <button
              key={pageIndex}
              type="button"
              className={pageIndex === page ? 'active' : ''}
              onClick={() => onGoToPage(pageIndex)}
            >
              {pageIndex + 1}
            </button>
          ))}
        </div>

        <button type="button" onClick={onNext} disabled={!hasNext}>
          Siguiente
        </button>
      </div>
    </div>
  )
}