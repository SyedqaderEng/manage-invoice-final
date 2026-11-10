import { describe, expect, it } from 'vitest'
import { formatPlanPrice } from './pricing'

describe('formatPlanPrice', () => {
  it('formats plan prices from cents as USD', () => {
    expect(formatPlanPrice(1900)).toBe('$19.00')
    expect(formatPlanPrice(49000)).toBe('$490.00')
  })
})
