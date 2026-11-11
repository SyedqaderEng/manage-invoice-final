import { describe, expect, it } from 'vitest'
import { recordsToCsv } from './recordsCsv'

const data = {
  vendor: '=HYPERLINK("x")', receiptNumber: null, transactionDate: '2026-03-12', transactionTime: null, currency: 'USD',
  subtotal: 69, tax: 5.52, discount: null, total: 74.52, paymentInfo: null, suggestedCategory: null, categoryId: null,
  items: [{ originalDescription: 'HP ENVY 4512', description: 'HP ENVY 4512', verified: false, quantity: null, unitPrice: null, lineTotal: 69 }],
}

describe('recordsToCsv', () => {
  it('exports confirmed records and neutralises formulas', () => {
    const lines = recordsToCsv([{ documentId: '1', filename: 'walmart, test.png', category: null, data }]).trim().split('\r\n')
    expect(lines).toHaveLength(2)
    expect(lines[1]).toBe('"walmart, test.png","\'=HYPERLINK(""x"")",2026-03-12,,,,USD,69,5.52,,74.52,HP ENVY 4512 (69.00)')
  })
})
