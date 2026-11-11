import { useEffect, useRef, useState } from 'react'
import { Button } from '../../components/ui/Button'
import { missingKeyFields, receiptDiscrepancies } from './receiptChecks'

export interface ReceiptItem {
  originalDescription: string
  description: string
  verified: boolean
  quantity: number | null
  unitPrice: number | null
  lineTotal: number | null
}

export interface ReceiptData {
  vendor: string | null
  receiptNumber: string | null
  transactionDate: string | null
  transactionTime: string | null
  currency: string | null
  subtotal: number | null
  tax: number | null
  discount: number | null
  total: number | null
  paymentInfo: string | null
  items: ReceiptItem[]
  suggestedCategory?: string | null
  categoryId?: string | null
}

export interface ReceiptRecord {
  status: 'DRAFT' | 'CONFIRMED'
  version: number
  updatedAt: string
  data: ReceiptData
}

type Api = <T>(path: string, options?: RequestInit, token?: string) => Promise<T>
interface Category { id: string; name: string }

export function ReceiptEditor({ documentId, filename, contentUrl, mimeType, initial, token, api, onClose, queued }: {
  documentId: string; filename: string; contentUrl: string; mimeType: string; initial: ReceiptRecord
  token: string; api: Api; onClose: () => void; queued: number
}) {
  const [record, setRecord] = useState(initial)
  const [data, setData] = useState(initial.data)
  const [saving, setSaving] = useState(false)
  const [recognizing, setRecognizing] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [original, setOriginal] = useState('')
  const [originalError, setOriginalError] = useState('')
  const [categories, setCategories] = useState<Category[]>([])
  const [categoryError, setCategoryError] = useState('')
  const [categoryName, setCategoryName] = useState('')
  const [creatingCategory, setCreatingCategory] = useState(false)
  const form = useRef<HTMLFormElement>(null)

  useEffect(() => {
    let active = true
    let objectUrl = ''
    fetch(contentUrl, { headers: { Authorization: `Bearer ${token}` } })
      .then(response => {
        if (!response.ok) throw new Error('Unable to load the original document.')
        return response.blob()
      })
      .then(blob => {
        objectUrl = URL.createObjectURL(blob)
        if (active) setOriginal(objectUrl)
        else URL.revokeObjectURL(objectUrl)
      })
      .catch(reason => { if (active) setOriginalError(reason instanceof Error ? reason.message : 'Unable to load the original document.') })
    return () => { active = false; if (objectUrl) URL.revokeObjectURL(objectUrl) }
  }, [contentUrl, token])

  useEffect(() => {
    let active = true
    api<Category[]>('/api/v1/categories', {}, token)
      .then(result => { if (active) setCategories(result) })
      .catch(reason => { if (active) setCategoryError(reason instanceof Error ? reason.message : 'Unable to load categories.') })
    return () => { active = false }
  }, [api, token])

  const text = (value: string) => value || null
  const numeric = (value: string) => value === '' ? null : Number(value)
  const updateItem = (index: number, patch: Partial<ReceiptItem>) => {
    setData(current => ({ ...current, items: current.items.map((item, i) => i === index ? { ...item, ...patch } : item) }))
  }
  const save = async (confirmed: boolean) => {
    if (saving || recognizing || creatingCategory) return
    if (!form.current?.reportValidity()) return
    setSaving(true)
    setError('')
    setMessage('')
    try {
      const saved = await api<ReceiptRecord>(`/api/v1/documents/${documentId}/record`, {
        method: 'PUT', body: JSON.stringify({ version: record.version, confirmed, data }),
      }, token)
      setRecord(saved)
      setData(saved.data)
      setMessage(confirmed ? 'Digital record confirmed and saved.' : 'Draft saved.')
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Unable to save the digital record.')
    } finally { setSaving(false) }
  }
  const regenerate = async () => {
    if (saving || recognizing || creatingCategory || !window.confirm('Replace the fields in this editor with new suggestions? Your saved record will not change until you save.')) return
    setRecognizing(true)
    setError('')
    setMessage('')
    try {
      const suggestions = await api<ReceiptData>(`/api/v1/documents/${documentId}/record/suggestions`, { method: 'POST' }, token)
      setData({ ...suggestions, categoryId: data.categoryId })
      setMessage('New suggestions are not saved yet. Review them before saving.')
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Unable to generate new suggestions.')
    } finally { setRecognizing(false) }
  }
  const createCategory = async () => {
    const name = categoryName.trim()
    if (!name || creatingCategory) return
    const existing = categories.find(category => category.name.toLocaleLowerCase() === name.toLocaleLowerCase())
    if (existing) {
      setData(current => ({ ...current, categoryId: existing.id }))
      setCategoryName('')
      setCategoryError('')
      return
    }
    setCreatingCategory(true)
    setCategoryError('')
    try {
      const created = await api<Category>('/api/v1/categories', { method: 'POST', body: JSON.stringify({ name }) }, token)
      setCategories(current => [...current, created])
      setData(current => ({ ...current, categoryId: created.id }))
      setCategoryName('')
    } catch (reason) {
      setCategoryError(reason instanceof Error ? reason.message : 'Unable to create the category.')
    } finally { setCreatingCategory(false) }
  }
  const matchedCategory = categories.find(category =>
    category.name.toLocaleLowerCase() === data.suggestedCategory?.trim().toLocaleLowerCase())
  const money = (label: string, field: 'subtotal' | 'tax' | 'discount' | 'total') => (
    <label className="field">{label}<input type="number" min="0" max="999999999999.99" step="0.01"
      value={data[field] ?? ''} onChange={event => setData({ ...data, [field]: numeric(event.target.value) })} /></label>
  )
  const { amountMismatch, lineMismatch } = receiptDiscrepancies(data)
  const missing = missingKeyFields(data)

  return <div className="document-preview-backdrop">
    <section className="receipt-editor" role="dialog" aria-modal="true" aria-label={`Digital record for ${filename}`}>
      <header><div><h2>Review digital record</h2><p>{filename} · {record.status === 'CONFIRMED' ? 'Confirmed' : 'Draft'}</p></div><button type="button" className="document-preview-close" aria-label="Close digital record" disabled={saving || recognizing || creatingCategory} onClick={() => { if (JSON.stringify(data) === JSON.stringify(record.data) || window.confirm('Discard unsaved changes?')) onClose() }}>×</button></header>
      <div className="receipt-editor-body">
        <div className="receipt-original"><h3>Original document</h3>
          {originalError ? <p role="alert">{originalError}</p> : !original ? <p role="status">Loading original…</p>
            : mimeType.startsWith('image/') ? <img src={original} alt={`Original ${filename}`} />
              : <iframe src={original} title={`Original ${filename}`} />}
        </div>
        <form className="receipt-fields" ref={form} onSubmit={event => event.preventDefault()}>
          <p>Automatic suggestions can be wrong or missing. Check every field against the original before confirming. Blank amounts are unknown, not zero.</p>
          {queued > 0 ? <p>{queued} more record{queued === 1 ? '' : 's'} ready to review. Save this record, then close it to open the next.</p> : null}
          <Button type="button" variant="secondary" disabled={saving || recognizing || creatingCategory} onClick={() => void regenerate()}>{recognizing ? 'Analyzing…' : 'Re-extract suggestions'}</Button>
          <fieldset className="receipt-edit-fields" disabled={saving || recognizing || creatingCategory}>
          <div className="receipt-grid">
            <label className="field">Category<select value={data.categoryId ?? ''} onChange={e => setData({ ...data, categoryId: e.target.value || null })}>
              <option value="">Uncategorized</option>
              {data.categoryId && !categories.some(category => category.id === data.categoryId) ? <option value={data.categoryId}>Previously selected category (unavailable)</option> : null}
              {categories.map(category => <option key={category.id} value={category.id}>{category.name}</option>)}
            </select></label>
            <label className="field">Vendor<input maxLength={255} value={data.vendor ?? ''} onChange={e => setData({ ...data, vendor: text(e.target.value) })} /></label>
            <label className="field">Receipt / invoice number<input maxLength={100} value={data.receiptNumber ?? ''} onChange={e => setData({ ...data, receiptNumber: text(e.target.value) })} /></label>
            <label className="field">Transaction date<input type="date" value={data.transactionDate ?? ''} onChange={e => setData({ ...data, transactionDate: text(e.target.value) })} /></label>
            <label className="field">Transaction time<input maxLength={50} value={data.transactionTime ?? ''} onChange={e => setData({ ...data, transactionTime: text(e.target.value) })} /></label>
            <label className="field">Currency (ISO code)<input maxLength={3} pattern="[A-Z]{3}" placeholder="e.g. USD" value={data.currency ?? ''} onChange={e => setData({ ...data, currency: text(e.target.value.toUpperCase()) })} /></label>
            <label className="field">Payment information<input maxLength={255} value={data.paymentInfo ?? ''} onChange={e => setData({ ...data, paymentInfo: text(e.target.value) })} /></label>
            {money('Subtotal', 'subtotal')}{money('Tax', 'tax')}{money('Discount', 'discount')}{money('Total', 'total')}
          </div>
          {data.suggestedCategory ? <p>AI-suggested category: <strong>{data.suggestedCategory}</strong>. Review before applying.
            {matchedCategory ? <button type="button" className="text-link" onClick={() => setData({ ...data, categoryId: matchedCategory.id })}> Use existing category</button>
              : <button type="button" className="text-link" onClick={() => setCategoryName(data.suggestedCategory ?? '')}> Use as new category name</button>}</p> : null}
          <label className="field">New category name<input maxLength={100} value={categoryName} onChange={e => setCategoryName(e.target.value)} /></label>
          <Button type="button" variant="secondary" disabled={!categoryName.trim() || creatingCategory} onClick={() => void createCategory()}>{creatingCategory ? 'Creating…' : 'Create or select category'}</Button>
          {categoryError ? <p className="form-error" role="alert">{categoryError}</p> : null}
          {missing.length ? <p className="form-error" role="status">Needs review, not found on the document: {missing.join(', ')}. Fill these in from the original. If a receipt only shows a $ sign, the currency is suggested as USD; change it if that is wrong.</p> : null}
          {amountMismatch ? <p className="form-error" role="alert">Subtotal plus tax minus discount does not match the total. Review the original.</p> : null}
          {lineMismatch ? <p className="form-error" role="alert">The listed line totals do not match the subtotal. Some items or adjustments may be missing.</p> : null}
          <h3>Items and services</h3>
          {data.items.map((item, index) => <fieldset className="receipt-item" key={index}>
            <legend>Item {index + 1}</legend>
            <label className="field">Original description<input maxLength={500} value={item.originalDescription} onChange={e => updateItem(index, { originalDescription: e.target.value })} /></label>
            <label className="field">Readable description<input maxLength={500} value={item.description} onChange={e => updateItem(index, { description: e.target.value })} /></label>
            <label><input type="checkbox" checked={item.verified} onChange={e => updateItem(index, { verified: e.target.checked })} /> Description verified</label>
            <div className="receipt-grid">{(['quantity', 'unitPrice', 'lineTotal'] as const).map(field => <label className="field" key={field}>{field === 'quantity' ? 'Quantity' : field === 'unitPrice' ? 'Unit price' : 'Line total'}
              <input type="number" min="0" max="999999999999.99" step={field === 'quantity' ? '0.0001' : '0.01'} value={item[field] ?? ''} onChange={e => updateItem(index, { [field]: numeric(e.target.value) })} /></label>)}</div>
            <button type="button" className="text-link" onClick={() => setData({ ...data, items: data.items.filter((_, i) => i !== index) })}>Remove item</button>
          </fieldset>)}
          <Button type="button" variant="secondary" disabled={data.items.length >= 200} onClick={() => setData({ ...data, items: [...data.items, { originalDescription: '', description: '', verified: false, quantity: null, unitPrice: null, lineTotal: null }] })}>Add item</Button>
          </fieldset>
          {error ? <p className="form-error" role="alert">{error} If another session changed it, close and reopen the record.</p> : null}
          {message ? <p className="form-success" role="status">{message}</p> : null}
          <div className="receipt-actions"><Button type="button" variant="secondary" disabled={saving || recognizing || creatingCategory} onClick={() => void save(false)}>Save draft</Button><Button type="button" disabled={saving || recognizing || creatingCategory} onClick={() => void save(true)}>{saving ? 'Saving…' : 'Confirm & save'}</Button></div>
        </form>
      </div>
    </section>
  </div>
}
