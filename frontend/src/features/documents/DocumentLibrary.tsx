import { useEffect, useRef, useState } from 'react'
import { Download, Eye, FileText, Plus, ScanLine, Trash2, Upload, X } from 'lucide-react'
import { Button } from '../../components/ui/Button'
import { mergePage } from './pagination'
import { ReceiptEditor, type ReceiptRecord } from './ReceiptEditor'
import { ApiError, ApiNetworkError } from '../../api'

interface DocumentRecord {
  id: string
  filename: string
  mimeType: string
  fileSize: number
  documentType: string
  processingStatus: string
  uploadedAt: string
  contentUrl: string
  downloadUrl: string
}

interface DocumentPage {
  content: DocumentRecord[]
  number: number
  totalPages: number
  totalElements: number
}

interface UploadResult {
  document: DocumentRecord
  alreadyUploaded: boolean
}

type Api = <T,>(path: string, options?: RequestInit, token?: string) => Promise<T>

const allowedTypes = ['application/pdf', 'image/jpeg', 'image/png', 'image/webp']
const maxFileSize = 25 * 1024 * 1024

export function DocumentLibrary({ token, api }: { token: string; api: Api }) {
  const [page, setPage] = useState<DocumentPage | null>(null)
  const [pageNumber, setPageNumber] = useState(0)
  const [refreshKey, setRefreshKey] = useState(0)
  const [files, setFiles] = useState<File[]>([])
  const fileInput = useRef<HTMLInputElement>(null)
  const [loading, setLoading] = useState(true)
  const [listError, setListError] = useState('')
  const [uploading, setUploading] = useState(false)
  const [uploadProgress, setUploadProgress] = useState('')
  const [deletingId, setDeletingId] = useState('')
  const [error, setError] = useState('')
  const [preview, setPreview] = useState<{ document: DocumentRecord; url: string } | null>(null)
  const [previewLoading, setPreviewLoading] = useState(false)
  const [ocrLoadingId, setOcrLoadingId] = useState('')
  const [ocrResult, setOcrResult] = useState<{ document: DocumentRecord; text: string; processedAt: string } | null>(null)
  const [receipts, setReceipts] = useState<{ document: DocumentRecord; record: ReceiptRecord }[]>([])
  const receipt = receipts[0]
  const [receiptLoadingId, setReceiptLoadingId] = useState('')
  const [manualDocument, setManualDocument] = useState<DocumentRecord | null>(null)

  useEffect(() => {
    let active = true
    setLoading(true)
    setListError('')
    api<DocumentPage>(`/api/v1/documents?page=${pageNumber}&size=20`, {}, token)
      .then(result => {
        if (active) setPage(previous => pageNumber === 0 || !previous ? result : {
          ...result, content: mergePage(previous.content, result.content),
        })
      })
      .catch(reason => { if (active) setListError(reason instanceof Error ? reason.message : 'Unable to load documents') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [api, pageNumber, refreshKey, token])

  const reload = () => {
    setPage(null)
    setPageNumber(0)
    setRefreshKey(value => value + 1)
  }

  const loadOrCreateReceipt = async (document: DocumentRecord) => {
    try {
      const record = await api<ReceiptRecord>(`/api/v1/documents/${document.id}/record`, {}, token)
      return { record, manualEntry: false }
    } catch (reason) {
      if (!(reason instanceof ApiError && reason.status === 404)) throw reason
    }

    try {
      const record = await api<ReceiptRecord>(`/api/v1/documents/${document.id}/record`, { method: 'POST' }, token)
      return { record, manualEntry: false }
    } catch (reason) {
      if (reason instanceof ApiError && reason.status === 409) {
        const record = await api<ReceiptRecord>(`/api/v1/documents/${document.id}/record`, {}, token)
        return { record, manualEntry: false }
      }
      if (!(reason instanceof ApiError && reason.status === 503)) throw reason
    }

    try {
      const record = await api<ReceiptRecord>(`/api/v1/documents/${document.id}/record?recognize=false`, { method: 'POST' }, token)
      return { record, manualEntry: true }
    } catch (reason) {
      if (reason instanceof ApiError && reason.status === 409) {
        const record = await api<ReceiptRecord>(`/api/v1/documents/${document.id}/record`, {}, token)
        return { record, manualEntry: false }
      }
      throw reason
    }
  }

  useEffect(() => () => {
    if (preview) URL.revokeObjectURL(preview.url)
  }, [preview])

  const upload = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!files.length || uploading) return
    const invalid = files.find(file => (
      (!allowedTypes.includes(file.type) && !/\.(pdf|jpe?g|png|webp)$/i.test(file.name))
      || file.size === 0
      || file.size > maxFileSize
    ))
    if (invalid) {
      setError(`${invalid.name} must be a PDF, JPEG, PNG, or WebP file no larger than 25 MB.`)
      return
    }

    setError('')
    setUploading(true)
    let uploadedCount = 0
    let existingCount = 0
    const processingFailures: string[] = []
    const recognitionNotes: string[] = []
    try {
      for (const [index, file] of files.entries()) {
        setUploadProgress(`Uploading ${index + 1} of ${files.length}: ${file.name}`)
        const body = new FormData()
        body.append('file', file)
        const result = await api<UploadResult>('/api/v1/documents', { method: 'POST', body }, token)
        const document = result.document
        uploadedCount += 1
        if (result.alreadyUploaded) existingCount += 1
        setUploadProgress(`Preparing digital record ${index + 1} of ${files.length}: ${file.name}`)
        try {
          const { record, manualEntry } = await loadOrCreateReceipt(document)
          setReceipts(current => [...current, { document, record }])
          if (manualEntry) {
            recognitionNotes.push(`${file.name}: Automatic extraction was unavailable; a blank draft is ready for manual entry.`)
          }
        } catch (reason) {
          processingFailures.push(`${file.name}: ${reason instanceof Error ? reason.message : 'Recognition failed'}`)
        }
      }
      setFiles([])
      if (fileInput.current) fileInput.current.value = ''
      const newCount = uploadedCount - existingCount
      const summary = existingCount === uploadedCount
        ? `${existingCount} matching existing document${existingCount === 1 ? '' : 's'} opened for review.`
        : existingCount
          ? `${newCount} new and ${existingCount} existing documents ready to review.`
          : `${uploadedCount} document${uploadedCount === 1 ? '' : 's'} uploaded.`
      setUploadProgress(`${summary} ${recognitionNotes.join(' ')}`.trim())
      if (processingFailures.length) setError(processingFailures.join(' '))
    } catch (reason) {
      if (uploadedCount) {
        setFiles(files.slice(uploadedCount))
        if (fileInput.current) fileInput.current.value = ''
        setUploadProgress(`${uploadedCount} of ${files.length} files uploaded; the remaining files are still selected.`)
      } else {
        setUploadProgress('')
      }
      const message = reason instanceof ApiNetworkError
        ? `${reason.message} The upload outcome is unknown; refresh the document list before retrying.`
        : reason instanceof Error ? reason.message : 'Unable to upload documents'
      setError([...processingFailures, message].join(' '))
    } finally {
      setUploading(false)
      if (uploadedCount) reload()
    }
  }

  const openReceipt = async (document: DocumentRecord, manual = false) => {
    if (receiptLoadingId) return
    setError('')
    setManualDocument(null)
    setReceiptLoadingId(document.id)
    try {
      let record: ReceiptRecord
      if (manual) {
        record = await api<ReceiptRecord>(`/api/v1/documents/${document.id}/record?recognize=false`, { method: 'POST' }, token)
      } else {
        try {
          record = await api<ReceiptRecord>(`/api/v1/documents/${document.id}/record`, {}, token)
        } catch (reason) {
          if (!(reason instanceof ApiError && reason.status === 404)) throw reason
          record = await api<ReceiptRecord>(`/api/v1/documents/${document.id}/record`, { method: 'POST' }, token)
        }
      }
      setReceipts(current => [...current, { document, record }])
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Unable to open the digital record.')
      if (!manual && !(reason instanceof ApiError && reason.status === 409)) setManualDocument(document)
    } finally { setReceiptLoadingId('') }
  }

  const fetchOriginal = async (document: DocumentRecord, mode: 'preview' | 'download') => {
    setError('')
    if (mode === 'preview') setPreviewLoading(true)
    try {
      const response = await fetch(document[mode === 'preview' ? 'contentUrl' : 'downloadUrl'], {
        headers: { Authorization: `Bearer ${token}` },
      })
      if (!response.ok) {
        const body = await response.json().catch(() => null)
        throw new Error(body?.detail || body?.message || 'Unable to retrieve this document')
      }
      const url = URL.createObjectURL(await response.blob())
      if (mode === 'preview') setPreview({ document, url })
      else {
        const link = window.document.createElement('a')
        link.href = url
        link.download = document.filename
        link.click()
        URL.revokeObjectURL(url)
      }
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Unable to retrieve this document')
    } finally {
      setPreviewLoading(false)
    }
  }

  const remove = async (document: DocumentRecord) => {
    if (!window.confirm(`Permanently delete "${document.filename}", its stored original and digital record?`)) return
    setError('')
    setDeletingId(document.id)
    try {
      await api<void>(`/api/v1/documents/${document.id}`, { method: 'DELETE' }, token)
      reload()
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Unable to delete this document')
    } finally {
      setDeletingId('')
    }
  }

  const readText = async (document: DocumentRecord, process: boolean) => {
    setError('')
    setOcrLoadingId(document.id)
    try {
      const result = await api<{ text: string; processedAt: string }>(
        `/api/v1/documents/${document.id}/ocr`, { method: process ? 'POST' : 'GET' }, token)
      setOcrResult({ document, ...result })
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Unable to read document text')
    } finally {
      setOcrLoadingId('')
    }
  }

  return (
    <section className="document-library">
      <header className="document-library-heading">
        <div><p className="section-kicker">YOUR LIBRARY</p><h1>Your personal documents</h1><p>Keep your receipts and invoices together with their original files.</p></div>
      </header>

      <form className="document-upload" onSubmit={upload}>
        <div className="upload-copy"><span className="upload-icon"><Upload size={19} /></span><div><strong>Add receipts and invoices</strong><p>PDF, JPEG, PNG, or WebP · up to 25 MB each</p></div></div>
        <label className="document-file-picker">
          <span><Plus size={16} /> Choose files</span>
          <input type="file" accept=".pdf,.jpg,.jpeg,.png,.webp,application/pdf,image/jpeg,image/png,image/webp" capture="environment" multiple disabled={uploading} onChange={event => {
            setFiles(Array.from(event.currentTarget.files || []))
            setError('')
            setUploadProgress('')
          }} />
        </label>
        {files.length ? <span className="selected-files">{files.length} file{files.length === 1 ? '' : 's'} selected</span> : null}
        <Button type="submit" disabled={!files.length || uploading}><Upload size={16} /> {uploading ? 'Uploading…' : 'Upload'}</Button>
      </form>

      {error ? <p className="document-message error" role="alert">{error}</p> : null}
      {manualDocument ? <p className="document-message">Recognition did not complete. <button type="button" className="text-link" onClick={() => void openReceipt(manualDocument, true)}>Create a blank record instead</button></p> : null}
      {uploadProgress ? <p className="document-message" role="status">{uploadProgress}</p> : null}

      <div className="document-list-heading"><div><h2>Recent uploads</h2>{page ? <p>{page.totalElements} document{page.totalElements === 1 ? '' : 's'}</p> : null}</div><Button variant="secondary" disabled={loading} onClick={reload}>Refresh list</Button></div>
      {page?.content.length ? (
        <div className="document-list">
          {page.content.map(document => (
            <article className="document-row" key={document.id}>
              <span className="document-file-icon"><FileText size={20} /></span>
              <div className="document-description"><strong>{document.filename}</strong><span>{new Date(document.uploadedAt).toLocaleDateString()} · {formatSize(document.fileSize)}</span><small>{document.processingStatus === 'UPLOADED' ? 'Original saved' : document.processingStatus}</small></div>
              <div className="document-actions">
                <button type="button" className="document-action" disabled={!!receiptLoadingId} onClick={() => void openReceipt(document)} aria-label={`Review digital record for ${document.filename}`}><FileText size={16} /><span>{receiptLoadingId === document.id ? 'Opening…' : 'Digital record'}</span></button>
                <button type="button" className="document-action" disabled={!!ocrLoadingId} onClick={() => void readText(document, true)} aria-label={`Recognize text in ${document.filename}`}><ScanLine size={16} /><span>{ocrLoadingId === document.id ? 'Reading…' : 'Read text'}</span></button>
                <button type="button" className="document-action" disabled={!!ocrLoadingId} onClick={() => void readText(document, false)} aria-label={`View saved text for ${document.filename}`}><span>Saved text</span></button>
                <button type="button" className="document-action" disabled={previewLoading} onClick={() => void fetchOriginal(document, 'preview')} aria-label={`View ${document.filename}`}><Eye size={16} /><span>View</span></button>
                <button type="button" className="document-action" onClick={() => void fetchOriginal(document, 'download')} aria-label={`Download ${document.filename}`}><Download size={16} /><span>Download</span></button>
                <button type="button" className="document-action danger" disabled={deletingId === document.id} onClick={() => void remove(document)} aria-label={`Delete ${document.filename}`}><Trash2 size={16} /><span>{deletingId === document.id ? 'Deleting…' : 'Delete'}</span></button>
              </div>
            </article>
          ))}
        </div>
      ) : page && !loading && !listError ? (
        <div className="document-empty"><span className="document-empty-icon"><FileText size={22} /></span><h2>Your library is ready</h2><p>Upload a receipt or invoice to keep its original safe and easy to find.</p></div>
      ) : null}
      {loading ? <p className="document-empty" role="status">Loading your documents…</p> : null}
      {listError ? <div className="document-message"><p role="alert">{listError}</p><Button variant="secondary" onClick={() => setRefreshKey(value => value + 1)}>Retry loading documents</Button></div> : null}
      {!loading && !listError && page && page.number + 1 < page.totalPages ? <Button variant="secondary" className="document-load-more" onClick={() => setPageNumber(page.number + 1)}>Load more</Button> : null}

      {preview ? (
        <div className="document-preview-backdrop" role="presentation" onClick={event => { if (event.target === event.currentTarget) setPreview(null) }}>
          <section className="document-preview" role="dialog" aria-modal="true" aria-label={`Preview ${preview.document.filename}`}>
            <header><strong>{preview.document.filename}</strong><button type="button" className="document-preview-close" aria-label="Close preview" onClick={() => setPreview(null)}><X size={19} /></button></header>
            {preview.document.mimeType.startsWith('image/') ? <img src={preview.url} alt={preview.document.filename} /> : <iframe src={preview.url} title={`Preview ${preview.document.filename}`} />}
          </section>
        </div>
      ) : null}
      {ocrResult ? (
        <div className="document-preview-backdrop" role="presentation" onClick={event => { if (event.target === event.currentTarget) setOcrResult(null) }}>
          <section className="document-preview document-ocr" role="dialog" aria-modal="true" aria-label={`Recognized text for ${ocrResult.document.filename}`}>
            <header><strong>Recognized text: {ocrResult.document.filename}</strong><button type="button" className="document-preview-close" aria-label="Close recognized text" onClick={() => setOcrResult(null)}><X size={19} /></button></header>
            <p>OCR runs locally and can make mistakes. This is unverified text, not a confirmed digital invoice.</p>
            <pre>{ocrResult.text}</pre>
          </section>
        </div>
      ) : null}
      {receipt ? <ReceiptEditor key={receipt.document.id} documentId={receipt.document.id} filename={receipt.document.filename}
        contentUrl={receipt.document.contentUrl} mimeType={receipt.document.mimeType}
        initial={receipt.record} token={token} api={api} queued={receipts.length - 1} onClose={() => setReceipts(current => current.slice(1))} /> : null}
    </section>
  )
}

function formatSize(bytes: number) {
  return bytes < 1024 * 1024 ? `${Math.max(1, Math.round(bytes / 1024))} KB` : `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}
