import { useEffect, useState } from 'react'
import { api } from '../../api'
import { Button } from '../../components/ui/Button'
import type { ReceiptData } from './ReceiptEditor'
import { recordsToCsv } from './recordsCsv'

function downloadCsv(records: RecordSummary[]) {
  const url = URL.createObjectURL(new Blob([recordsToCsv(records)], { type: 'text/csv;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = `confirmed-records-${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(url)
}

export interface RecordSummary { documentId: string; filename: string; category: string | null; data: ReceiptData }
interface RecordPage { content: RecordSummary[]; number: number; totalPages: number }
type Group = { label: string; cents: bigint }

export function money(cents: bigint) {
  return `${cents / 100n}.${(cents % 100n).toString().padStart(2, '0')}`
}

export function grouped(records: RecordSummary[], key: (record: RecordSummary) => string | null): Group[] {
  const amounts = new Map<string, bigint>()
  for (const record of records) {
    const label = key(record)
    if (label === null || record.data.total === null) continue
    amounts.set(label, (amounts.get(label) ?? 0n) + BigInt(Math.round(record.data.total * 100)))
  }
  return [...amounts.entries()].map(([label, cents]) => ({ label, cents })).sort((a, b) => a.label.localeCompare(b.label))
}

function Bars({ title, groups }: { title: string; groups: Group[] }) {
  const max = groups.reduce((largest, group) => group.cents > largest ? group.cents : largest, 0n)
  return <section className="spending-card"><h3>{title}</h3>
    {!groups.length ? <p>No matching amounts yet.</p> : <ul className="spending-bars">
      {groups.map(group => <li key={group.label}><span>{group.label}</span>
        <div className="spending-track" aria-hidden="true"><div style={{ width: `${max ? Number(group.cents * 10000n / max) / 100 : 0}%` }} /></div>
        <strong>{money(group.cents)}</strong></li>)}
    </ul>}
  </section>
}

export function SpendingDashboard({ token }: { token: string }) {
  const [records, setRecords] = useState<RecordSummary[] | null>(null)
  const [error, setError] = useState('')
  const [retry, setRetry] = useState(0)
  const [currency, setCurrency] = useState('')
  useEffect(() => {
    let active = true
    setRecords(null)
    setError('')
    async function load() {
      const all: RecordSummary[] = []
      let page = 0
      let totalPages: number
      do {
        const result = await api<RecordPage>(`/api/v1/documents/records/confirmed?page=${page}`, {}, token)
        if (result.number !== page || !Array.isArray(result.content) || !Number.isInteger(result.totalPages)) throw new Error('Invalid records response.')
        all.push(...result.content)
        totalPages = result.totalPages
        page += 1
      } while (page < totalPages)
      if (active) setRecords(all)
    }
    load().catch(reason => { if (active) setError(reason instanceof Error ? reason.message : 'Unable to load records.') })
    return () => { active = false }
  }, [token, retry])
  if (error) return <section className="spending-card"><p role="alert">{error}</p><Button variant="secondary" onClick={() => setRetry(value => value + 1)}>Retry dashboard</Button></section>
  if (!records) return <p role="status">Loading confirmed records…</p>
  const currencies = [...new Set(records.map(record => record.data.currency).filter((value): value is string => !!value))].sort()
  const selected = currencies.includes(currency) ? currency : currencies[0]
  const matching = records.filter(record => record.data.currency === selected && record.data.total !== null)
  const total = matching.reduce((sum, record) => sum + BigInt(Math.round((record.data.total ?? 0) * 100)), 0n)
  const withoutCurrency = records.filter(record => record.data.currency === null && record.data.total !== null).length
  const withoutTotal = records.filter(record => record.data.total === null).length
  return <section className="spending-overview">
    <h2>Confirmed spending</h2>
    <p>Only confirmed records are included. Amounts are grouped by currency; missing values are not treated as zero.</p>
    {currencies.length ? <label className="field">Currency<select value={selected} onChange={event => setCurrency(event.target.value)}>{currencies.map(code => <option key={code}>{code}</option>)}</select></label> : <p>No confirmed amounts with a currency yet.</p>}
    {selected ? <p>Total ({selected}): <strong>{money(total)}</strong> across {matching.length} record{matching.length === 1 ? '' : 's'}.</p> : null}
    {(withoutCurrency > 0 || withoutTotal > 0) ? <p>{withoutCurrency} record(s) with unknown currency and {withoutTotal} with unknown total are excluded from totals.</p> : null}
    {selected ? <div className="spending-grid">
      <Bars title={`By category (${selected})`} groups={grouped(matching, record => record.category ?? 'Uncategorized')} />
      <Bars title={`By month (${selected})`} groups={grouped(matching, record => record.data.transactionDate?.slice(0, 7) ?? null)} />
    </div> : null}
    {selected && matching.some(record => !record.data.transactionDate) ? <p>Records without a transaction date are excluded from the monthly chart.</p> : null}
    <section className="spending-card"><h3>Confirmed records</h3>
      <Button variant="secondary" disabled={records.length === 0} onClick={() => downloadCsv(records)}>Export CSV</Button>
      {records.length === 0 ? <p>Confirm a digital record to see it here.</p> : <div className="spending-table-wrap"><table>
        <thead><tr><th>Document</th><th>Vendor</th><th>Date</th><th>Category</th><th>Total</th></tr></thead>
        <tbody>{records.map(record => <tr key={record.documentId}><td>{record.filename}</td><td>{record.data.vendor ?? 'Unknown'}</td>
          <td>{record.data.transactionDate ?? 'Unknown'}</td><td>{record.category ?? 'Uncategorized'}</td>
          <td>{record.data.total === null ? 'Unknown' : `${record.data.currency ?? 'Currency unknown'} ${money(BigInt(Math.round(record.data.total * 100)))}`}</td></tr>)}</tbody>
      </table></div>}
    </section>
  </section>
}
