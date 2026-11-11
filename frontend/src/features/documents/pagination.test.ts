import { describe, expect, it } from 'vitest'
import { mergePage } from './pagination'

describe('mergePage', () => {
  it('keeps earlier records in order and appends the next page', () => {
    expect(mergePage([{ id: 'a' }, { id: 'b' }], [{ id: 'c' }, { id: 'd' }]))
      .toEqual([{ id: 'a' }, { id: 'b' }, { id: 'c' }, { id: 'd' }])
  })

  it('ignores repeated records from shifting or repeated pages', () => {
    expect(mergePage([{ id: 'a' }], [{ id: 'a' }, { id: 'b' }, { id: 'b' }]))
      .toEqual([{ id: 'a' }, { id: 'b' }])
  })
})
