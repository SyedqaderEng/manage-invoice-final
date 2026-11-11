import { describe, expect, it } from 'vitest'
import { grouped, money, type RecordSummary } from './SpendingDashboard'
import type { ReceiptData } from './ReceiptEditor'

const data: ReceiptData = {
  vendor: null, receiptNumber: null, transactionDate: null, transactionTime: null,
  currency: 'USD', subtotal: null, tax: null, discount: null, total: null, paymentInfo: null, items: [],
}
const record = (id: string, total: number | null, category: string | null, date: string | null): RecordSummary => ({
  documentId: id, filename: id, category, data: { ...data, total, transactionDate: date },
})

describe('confirmed spending groups', () => {
  it('sums cents accurately and omits unknown totals', () => {
    const records = [record('1', 0.1, 'Food', '2026-01-01'), record('2', 0.2, 'Food', null),
      record('3', null, 'Food', '2026-01-02'), record('4', 1, null, '2026-02-02')]
    expect(grouped(records, entry => entry.category ?? 'Uncategorized')).toEqual([
      { label: 'Food', cents: 30n }, { label: 'Uncategorized', cents: 100n },
    ])
    expect(grouped(records, entry => entry.data.transactionDate?.slice(0, 7) ?? null)).toEqual([
      { label: '2026-01', cents: 10n }, { label: '2026-02', cents: 100n },
    ])
    expect(money(30n)).toBe('0.30')
    expect(money(100000000000001n)).toBe('1000000000000.01')
  })
})
