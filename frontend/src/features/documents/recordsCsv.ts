import type { ReceiptData } from './ReceiptEditor'

export interface CsvRecord { documentId: string; filename: string; category: string | null; data: ReceiptData }

/** Quote a cell and neutralise spreadsheet formulas (CSV injection). */
function cell(value: string | number | null | undefined): string {
  if (value === null || value === undefined) return ''
  let text = String(value)
  if (/^[=+\-@\t\r]/.test(text) && typeof value === 'string') text = `'${text}`
  return /[",\r\n]/.test(text) || text !== String(value) ? `"${text.replace(/"/g, '""')}"` : text
}

export function recordsToCsv(records: CsvRecord[]): string {
  const header = ['Document', 'Vendor', 'Date', 'Time', 'Receipt number', 'Category', 'Currency', 'Subtotal', 'Tax', 'Discount', 'Total', 'Items']
  const rows = records.map(({ filename, category, data }) => [
    filename, data.vendor, data.transactionDate, data.transactionTime, data.receiptNumber, category, data.currency,
    data.subtotal, data.tax, data.discount, data.total,
    data.items.map(item => `${item.description}${item.lineTotal === null ? '' : ` (${item.lineTotal.toFixed(2)})`}`).join('; '),
  ].map(cell).join(','))
  return [header.join(','), ...rows].join('\r\n') + '\r\n'
}
