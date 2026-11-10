import { useEffect, useState } from 'react'
import { Download, Eye, FileText, Plus, Trash2, Upload, X } from 'lucide-react'
import { Button } from '../../components/ui/Button'

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

type Api = <T,>(path: string, options?: RequestInit, token?: string) => Promise<T>

const allowedTypes = ['application/pdf', 'image/jpeg', 'image/png', 'image/webp']
const maxFileSize = 25 * 1024 * 1024

export function DocumentLibrary({ token, api }: { token: string; api: Api }) {
  const [page, setPage] = useState<DocumentPage | null>(null)
  const [pageNumber, setPageNumber] = useState(0)
  const [refreshKey, setRefreshKey] = useState(0)
  const [files, setFiles] = useState<File[]>([])
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)
  const [uploadProgress, setUploadProgress] = useState('')
  const [deletingId, setDeletingId] = useState('')
  const [error, setError] = useState('')
  const [preview, setPreview] = useState<{ document: DocumentRecord; url: string } | null>(null)
  const [previewLoading, setPreviewLoading] = useState(false)

  useEffect(() => {
    let active = true
    setLoading(true)
    api<DocumentPage>(`/api/v1/documents?page=${pageNumber}&size=20`, {}, token)
      .then(result => { if (active) setPage(result) })
      .catch(reason => { if (active) setError(reason instanceof Error ? reason.message : 'Unable to load documents') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [api, pageNumber, refreshKey, token])

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
    try {
      for (const [index, file] of files.entries()) {
        setUploadProgress(`Uploading ${index + 1} of ${files.length}: ${file.name}`)
        const body = new FormData()
        body.append('file', file)
        await api<DocumentRecord>('/api/v1/documents', { method: 'POST', body }, token)
        uploadedCount += 1
      }
      setFiles([])
      setUploadProgress(`${uploadedCount} document${uploadedCount === 1 ? '' : 's'} uploaded.`)
    } catch (reason) {
      if (uploadedCount) {
        setFiles(files.slice(uploadedCount))
        setUploadProgress(`${uploadedCount} of ${files.length} files uploaded; the remaining files are still selected.`)
      }
      setError(reason instanceof Error ? reason.message : 'Unable to upload documents')
    } finally {
      setUploading(false)
      if (uploadedCount) {
        setPageNumber(0)
        setRefreshKey(value => value + 1)
      }
    }
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
    if (!window.confirm(`Permanently delete "${document.filename}" and its stored original?`)) return
    setError('')
    setDeletingId(document.id)
    try {
      await api<void>(`/api/v1/documents/${document.id}`, { method: 'DELETE' }, token)
      setRefreshKey(value => value + 1)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Unable to delete this document')
    } finally {
      setDeletingId('')
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
          <input type="file" accept=".pdf,.jpg,.jpeg,.png,.webp,application/pdf,image/jpeg,image/png,image/webp" capture="environment" multiple onChange={event => {
            setFiles(Array.from(event.currentTarget.files || []))
            setError('')
            setUploadProgress('')
          }} />
        </label>
        {files.length ? <span className="selected-files">{files.length} file{files.length === 1 ? '' : 's'} selected</span> : null}
        <Button type="submit" disabled={!files.length || uploading}><Upload size={16} /> {uploading ? 'Uploading…' : 'Upload'}</Button>
      </form>

      {error ? <p className="document-message error" role="alert">{error}</p> : null}
      {uploadProgress ? <p className="document-message" role="status">{uploadProgress}</p> : null}

      <div className="document-list-heading"><div><h2>Recent uploads</h2><p>{page?.totalElements ?? 0} document{page?.totalElements === 1 ? '' : 's'}</p></div></div>
      {loading ? <p className="document-empty">Loading your documents…</p> : page?.content.length ? (
        <div className="document-list">
          {page.content.map(document => (
            <article className="document-row" key={document.id}>
              <span className="document-file-icon"><FileText size={20} /></span>
              <div className="document-description"><strong>{document.filename}</strong><span>{new Date(document.uploadedAt).toLocaleDateString()} · {formatSize(document.fileSize)}</span><small>{document.processingStatus === 'UPLOADED' ? 'Original saved' : document.processingStatus}</small></div>
              <div className="document-actions">
                <button type="button" className="document-action" disabled={previewLoading} onClick={() => void fetchOriginal(document, 'preview')} aria-label={`View ${document.filename}`}><Eye size={16} /><span>View</span></button>
                <button type="button" className="document-action" onClick={() => void fetchOriginal(document, 'download')} aria-label={`Download ${document.filename}`}><Download size={16} /><span>Download</span></button>
                <button type="button" className="document-action danger" disabled={deletingId === document.id} onClick={() => void remove(document)} aria-label={`Delete ${document.filename}`}><Trash2 size={16} /><span>{deletingId === document.id ? 'Deleting…' : 'Delete'}</span></button>
              </div>
            </article>
          ))}
        </div>
      ) : page ? (
        <div className="document-empty"><span className="document-empty-icon"><FileText size={22} /></span><h2>Your library is ready</h2><p>Upload a receipt or invoice to keep its original safe and easy to find.</p></div>
      ) : null}
      {page && page.number + 1 < page.totalPages ? <Button variant="secondary" className="document-load-more" onClick={() => setPageNumber(value => value + 1)}>Load more</Button> : null}

      {preview ? (
        <div className="document-preview-backdrop" role="presentation" onClick={event => { if (event.target === event.currentTarget) setPreview(null) }}>
          <section className="document-preview" role="dialog" aria-modal="true" aria-label={`Preview ${preview.document.filename}`}>
            <header><strong>{preview.document.filename}</strong><button type="button" className="document-preview-close" aria-label="Close preview" onClick={() => setPreview(null)}><X size={19} /></button></header>
            {preview.document.mimeType.startsWith('image/') ? <img src={preview.url} alt={preview.document.filename} /> : <iframe src={preview.url} title={`Preview ${preview.document.filename}`} />}
          </section>
        </div>
      ) : null}
    </section>
  )
}

function formatSize(bytes: number) {
  return bytes < 1024 * 1024 ? `${Math.max(1, Math.round(bytes / 1024))} KB` : `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}
