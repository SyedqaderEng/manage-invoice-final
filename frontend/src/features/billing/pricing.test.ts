import { describe, expect, it } from 'vitest'
import { formatPlanPrice, parsePersonalPlans } from './pricing'

describe('formatPlanPrice', () => {
  it('formats plan prices from cents as USD', () => {
    expect(formatPlanPrice(1900)).toBe('$19.00')
    expect(formatPlanPrice(49000)).toBe('$490.00')
  })

  describe('parsePersonalPlans', () => {
    const free = { id: 'free', name: 'Free', monthlyCents: 0, yearlyCents: 0, currency: 'usd', lifetimeFreeReceipts: 30, unlimitedReceipts: false, trialMonths: 0, checkoutAvailable: false }
    const pro = { id: 'pro', name: 'Pro', monthlyCents: 1000, yearlyCents: 6700, currency: 'usd', lifetimeFreeReceipts: null, unlimitedReceipts: true, trialMonths: 1, checkoutAvailable: false }
    it('accepts both personal plans with their exact monetary and trial terms', () => {
      expect(parsePersonalPlans([free, pro])).toEqual([free, pro])
      expect(formatPlanPrice(pro.monthlyCents)).toBe('$10.00')
      expect(formatPlanPrice(pro.yearlyCents)).toBe('$67.00')
    })
    it('rejects malformed, missing and duplicate plans rather than showing fallback prices', () => {
      expect(() => parsePersonalPlans([free])).toThrow()
      expect(() => parsePersonalPlans([free, free])).toThrow()
      expect(() => parsePersonalPlans([free, { ...pro, yearlyCents: '6700' }])).toThrow()
    })
  })
})
