import { describe, expect, it } from 'vitest'
import { receiptDiscrepancies } from './receiptChecks'
import type { ReceiptData } from './ReceiptEditor'

const base: ReceiptData = {
  vendor: null, receiptNumber: null, transactionDate: null, transactionTime: null,
  currency: null, subtotal: 0.3, tax: 0.02, discount: 0, total: 0.32, paymentInfo: null,
  items: [
    { originalDescription: 'a', description: 'a', verified: false, quantity: null, unitPrice: null, lineTotal: 0.1 },
    { originalDescription: 'b', description: 'b', verified: false, quantity: null, unitPrice: null, lineTotal: 0.2 },
  ],
}

describe('receiptDiscrepancies', () => {
  it('compares cents without floating point mismatch', () => {
    expect(receiptDiscrepancies(base)).toEqual({ amountMismatch: false, lineMismatch: false })
    expect(receiptDiscrepancies({ ...base, total: 0.33, subtotal: 0.31 })).toEqual({ amountMismatch: false, lineMismatch: true })
  })

  it('checks the totals when no discount is printed', () => {
    const walmart = { ...base, subtotal: 69, tax: 5.52, discount: null, total: 74.52, items: [] }
    expect(receiptDiscrepancies(walmart).amountMismatch).toBe(false)
    expect(receiptDiscrepancies({ ...walmart, total: 74.05 }).amountMismatch).toBe(true)
  })

  it('does not treat unknown amounts as zero', () => {
    expect(receiptDiscrepancies({ ...base, tax: null, total: 99 })).toEqual({ amountMismatch: false, lineMismatch: false })
    expect(receiptDiscrepancies({ ...base, items: [{ ...base.items[0], lineTotal: null }] }).lineMismatch).toBe(false)
  })
})
