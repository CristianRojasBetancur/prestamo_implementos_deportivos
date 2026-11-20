import { useState } from 'react'
import type { Condition, CreateReturnRequest, Loan } from '../services/api/types'
import './return-form.css'

type ReturnFormProps = {
  loan: Loan
  busy: boolean
  error: string
  onCancel: () => void
  onSubmit: (body: CreateReturnRequest) => Promise<void>
}

const pendingDetails = (loan: Loan) => loan.detalles.filter((detail) => detail.estado === 'PENDIENTE_DEVOLUCION')

export function ReturnFormPartial({ loan, busy, error, onCancel, onSubmit }: ReturnFormProps) {
  const pending = pendingDetails(loan)
  const [selectedIds, setSelectedIds] = useState<string[]>(() => pending.map((detail) => detail.implemento.id))
  const [conditions, setConditions] = useState<Record<string, Condition>>(() =>
    Object.fromEntries(pending.map((detail) => [detail.implemento.id, 'BUENO'])),
  )
  const [descriptions, setDescriptions] = useState<Record<string, string>>({})
  const [justified, setJustified] = useState(false)
  const [justification, setJustification] = useState('')

  const hasNovelty = selectedIds.some((id) => conditions[id] === 'CON_NOVEDAD')
  const canSubmit = !busy && selectedIds.length > 0

  function toggleUnit(id: string) {
    setSelectedIds((current) => (current.includes(id) ? current.filter((selectedId) => selectedId !== id) : [...current, id]))
  }

  function updateCondition(id: string, condition: Condition) {
    setConditions((current) => ({ ...current, [id]: condition }))
    if (condition === 'BUENO') {
      setDescriptions((current) => {
        const next = { ...current }
        delete next[id]
        return next
      })
    }
  }

  function submit() {
    void onSubmit({
      items: pending
        .filter((detail) => selectedIds.includes(detail.implemento.id))
        .map((detail) => ({
          implementoId: detail.implemento.id,
          condicion: conditions[detail.implemento.id],
          incidencia: conditions[detail.implemento.id] === 'CON_NOVEDAD'
            ? { tipo: 'DANO' as const, descripcion: descriptions[detail.implemento.id] ?? '' }
            : undefined,
        })),
      retrasoJustificado: justified,
      justificacion: justified ? justification : undefined,
    })
  }

  return <div className="data-panel wizard-panel return-form-panel">
    <div className="metric-strip">
      <strong>{loan.codigo}</strong>
      <span>{selectedIds.length} de {pending.length} unidad(es) seleccionada(s)</span>
    </div>
    <p className="panel-subtitle">Marca las unidades que fueron devueltas. Las unidades desmarcadas permanecerán pendientes.</p>
    {pending.map((detail) => {
      const id = detail.implemento.id
      const selected = selectedIds.includes(id)
      const condition = conditions[id]
      return <div className={`return-item ${condition === 'CON_NOVEDAD' && selected ? 'needs-review' : ''} ${!selected ? 'disabled' : ''}`} aria-disabled={!selected} key={detail.id}>
        <label className="return-selection">
          <input
            type="checkbox"
            checked={selected}
            onChange={() => toggleUnit(id)}
            aria-label={`Marcar devolución de ${detail.implemento.nombre} ${detail.implemento.codigoInventario}`}
          />
        </label>
        <div className="return-item-info">
          <strong>{detail.implemento.nombre}</strong>
          <span>{detail.implemento.codigoInventario}</span>
          <small>{selected ? condition === 'CON_NOVEDAD' ? 'La incidencia debe viajar en la misma solicitud.' : 'Volverá a disponible si el backend acepta la devolución.' : 'No se enviará en esta devolución.'}</small>
        </div>
        <label className="select-wrap">
          <span>Condición</span>
          <select disabled={!selected} value={condition} onChange={(event) => updateCondition(id, event.target.value as Condition)}>
            <option value="BUENO">Buen estado</option>
            <option value="CON_NOVEDAD">Con novedad</option>
          </select>
        </label>
        {condition === 'CON_NOVEDAD' && <textarea
          aria-label={`Descripción de ${detail.implemento.nombre}`}
          disabled={!selected}
          required={selected}
          value={descriptions[id] ?? ''}
          onChange={(event) => setDescriptions((current) => ({ ...current, [id]: event.target.value }))}
          placeholder="Describe la novedad"
          rows={2}
        />}
      </div>
    })}
    {!selectedIds.length && <div className="info-strip" role="status">Selecciona al menos una unidad para registrar la devolución.</div>}
    {loan.vencido && <div className="info-strip"><label className="form-field"><span><input type="checkbox" checked={justified} onChange={(event) => setJustified(event.target.checked)} /> Retraso justificado</span>{justified && <input required value={justification} onChange={(event) => setJustification(event.target.value)} placeholder="Justificación" />}</label></div>}
    {error && <p className="field-error" role="alert">{error}</p>}
    <div className="wizard-actions">
      <button className="secondary-button" onClick={onCancel}>Cancelar</button>
      <button disabled={!canSubmit} className="primary-button" onClick={submit}>{busy ? 'Registrando…' : hasNovelty ? 'Registrar devolución e incidencia' : 'Confirmar devolución'}</button>
    </div>
  </div>
}
