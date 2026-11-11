import type { ReceiptData } from './ReceiptEditor'

export function receiptDiscrepancies(data: ReceiptData) {
  const { subtotal, tax, discount, total, items } = data
  // A blank discount means none was printed, so it counts as 0 for the arithmetic check.
  const amountMismatch = subtotal !== null && tax !== null && total !== null
    && Math.round(subtotal * 100) + Math.round(tax * 100) - Math.round((discount ?? 0) * 100) !== Math.round(total * 100)
  const lineMismatch = items.length > 0 && items.every(item => item.lineTotal !== null) && subtotal !== null
    && items.reduce((sum, item) => sum + Math.round(item.lineTotal! * 100), 0) !== Math.round(subtotal * 100)
  return { amountMismatch, lineMismatch }
}

/** Key fields that are blank and should be checked against the original before confirming. */
export function missingKeyFields(data: ReceiptData): string[] {
  const missing: string[] = []
  if (!data.vendor) missing.push('Vendor')
  if (!data.transactionDate) missing.push('Transaction date')
  if (!data.currency) missing.push('Currency')
  if (data.total === null) missing.push('Total')
  return missing
}
