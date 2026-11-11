export interface UploadUsage {
  trackedUploads: number
  historyComplete: boolean
  limitEnforced: boolean
}

export function parseUploadUsage(value: unknown): UploadUsage {
  if (!value || typeof value !== 'object') throw new Error('Upload usage is unavailable.')
  const usage = value as Record<string, unknown>
  if (typeof usage.trackedUploads !== 'number' || !Number.isSafeInteger(usage.trackedUploads) || usage.trackedUploads < 0
    || typeof usage.historyComplete !== 'boolean' || typeof usage.limitEnforced !== 'boolean') {
    throw new Error('Upload usage is unavailable.')
  }
  return { trackedUploads: usage.trackedUploads, historyComplete: usage.historyComplete, limitEnforced: usage.limitEnforced }
}
