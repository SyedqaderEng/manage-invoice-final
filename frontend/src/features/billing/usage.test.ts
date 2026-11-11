import { describe, expect, it } from 'vitest'
import { parseUploadUsage } from './usage'

describe('parseUploadUsage', () => {
  it('accepts a complete usage response', () => {
    const usage = { trackedUploads: 31, historyComplete: false, limitEnforced: false }
    expect(parseUploadUsage(usage)).toEqual(usage)
  })

  it('rejects malformed or unsafe counts and missing flags', () => {
    for (const value of [null, {}, { trackedUploads: -1, historyComplete: true, limitEnforced: false },
      { trackedUploads: 1.5, historyComplete: true, limitEnforced: false },
      { trackedUploads: 0, historyComplete: true }]) {
      expect(() => parseUploadUsage(value)).toThrow()
    }
  })
})
