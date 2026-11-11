import { useCallback, useEffect, useState } from 'react'
import { Store, Tags } from 'lucide-react'
import { Button } from '../../components/ui/Button'

interface Category {
  id: string
  name: string
  description: string | null
  color: string | null
}

interface Vendor {
  id: string
  displayName: string
  email: string | null
  phone: string | null
  address: string | null
  defaultCategoryId: string | null
}

type Api = <T,>(path: string, options?: RequestInit, token?: string) => Promise<T>

export function CustomerDataPage({ section, token, api }: { section: 'vendors' | 'categories'; token: string; api: Api }) {
  const isVendors = section === 'vendors'
  const [vendors, setVendors] = useState<Vendor[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [editing, setEditing] = useState<Vendor | Category | null>(null)

  const refresh = useCallback(async () => {
    setError('')
    try {
      if (isVendors) {
        const [vendorData, categoryData] = await Promise.all([
          api<Vendor[]>('/api/v1/vendors', {}, token),
          api<Category[]>('/api/v1/categories', {}, token),
        ])
        setVendors(vendorData)
        setCategories(categoryData)
      } else {
        setCategories(await api<Category[]>('/api/v1/categories', {}, token))
      }
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : `Unable to load ${section}`)
    } finally {
      setLoading(false)
    }
  }, [api, isVendors, section, token])

  useEffect(() => { void refresh() }, [refresh])
  useEffect(() => { setEditing(null); setMessage(''); setLoading(true) }, [section])

  const create = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (saving) return
    const formElement = event.currentTarget
    const form = new FormData(formElement)
    const payload = isVendors
      ? {
          displayName: form.get('displayName'),
          email: form.get('email') || null,
          phone: form.get('phone') || null,
          address: form.get('address') || null,
          defaultCategoryId: form.get('defaultCategoryId') || null,
        }
      : { name: form.get('name'), description: form.get('description') || null, color: form.get('color') || null }
    setError('')
    setMessage('')
    setSaving(true)
    try {
      const base = isVendors ? '/api/v1/vendors' : '/api/v1/categories'
      await api(editing ? `${base}/${editing.id}` : base, {
        method: editing ? 'PATCH' : 'POST',
        body: JSON.stringify(payload),
      }, token)
      formElement.reset()
      setMessage(`${isVendors ? 'Vendor' : 'Category'} ${editing ? 'updated' : 'added'}.`)
      setEditing(null)
      await refresh()
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : `Unable to add ${isVendors ? 'vendor' : 'category'}`)
    } finally {
      setSaving(false)
    }
  }

  const deactivate = async (item: Vendor | Category) => {
    const name = 'displayName' in item ? item.displayName : item.name
    if (!window.confirm(`Deactivate ${name}? Existing records will be preserved.`)) return
    setError('')
    setMessage('')
    setSaving(true)
    try {
      await api(`/api/v1/${isVendors ? 'vendors' : 'categories'}/${item.id}`, { method: 'DELETE' }, token)
      if (editing?.id === item.id) setEditing(null)
      setMessage(`${isVendors ? 'Vendor' : 'Category'} deactivated.`)
      await refresh()
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Unable to deactivate')
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="customer-data-page">
      <header className="document-library-heading">
        <p className="section-kicker">YOUR ORGANIZATION</p>
        <h1>{isVendors ? 'Vendors' : 'Expense categories'}</h1>
        <p>{isVendors ? 'Keep track of the stores and service providers in your records.' : 'Create categories to organize your personal spending.'}</p>
      </header>
      <div className="customer-data-layout">
        <form key={`${section}-${editing?.id || 'new'}`} className="customer-data-form" onSubmit={create}>
          <div className="customer-data-form-title">{isVendors ? <Store size={18} /> : <Tags size={18} />}<strong>{editing ? 'Edit' : 'Add'} {isVendors ? 'a vendor' : 'a category'}</strong></div>
          {isVendors ? (
            <>
              <label className="field">Vendor name<input name="displayName" required maxLength={255} defaultValue={editing && 'displayName' in editing ? editing.displayName : ''} /></label>
              <label className="field">Email (optional)<input name="email" type="email" maxLength={320} defaultValue={editing && 'email' in editing ? editing.email || '' : ''} /></label>
              <label className="field">Phone (optional)<input name="phone" maxLength={50} defaultValue={editing && 'phone' in editing ? editing.phone || '' : ''} /></label>
              <label className="field">Address (optional)<textarea name="address" maxLength={2000} rows={3} defaultValue={editing && 'address' in editing ? editing.address || '' : ''} /></label>
              <label className="field">Default category (optional)<select name="defaultCategoryId" defaultValue={editing && 'defaultCategoryId' in editing ? editing.defaultCategoryId || '' : ''}><option value="">No default</option>{categories.map(category => <option value={category.id} key={category.id}>{category.name}</option>)}</select></label>
            </>
          ) : (
            <>
              <label className="field">Category name<input name="name" required maxLength={100} defaultValue={editing && 'name' in editing ? editing.name : ''} /></label>
              <label className="field">Description (optional)<textarea name="description" maxLength={2000} rows={3} defaultValue={editing && 'description' in editing ? editing.description || '' : ''} /></label>
              <label className="field">Color (optional)<input name="color" type="color" defaultValue={editing && 'color' in editing ? editing.color || '#6375d5' : '#6375d5'} /></label>
            </>
          )}
          {error ? <p className="form-error" role="alert">{error}</p> : null}
          {message ? <p className="form-success" role="status">{message}</p> : null}
          <Button type="submit" disabled={saving}>{saving ? 'Saving…' : editing ? 'Save changes' : `Add ${isVendors ? 'vendor' : 'category'}`}</Button>
          {editing ? <button type="button" className="text-link" onClick={() => setEditing(null)}>Cancel editing</button> : null}
        </form>

        <section className="customer-data-list" aria-label={isVendors ? 'Your vendors' : 'Your categories'}>
          <h2>{isVendors ? 'Your vendors' : 'Your categories'}</h2>
          {loading ? <p className="document-empty">Loading…</p> : (
            <div className="customer-data-items">
              {(isVendors ? vendors : categories).length ? (
                isVendors
                  ? vendors.map(vendor => <article className="customer-data-item" key={vendor.id}><span className="customer-data-item-icon"><Store size={17} /></span><div><strong>{vendor.displayName}</strong><small>{vendor.email || vendor.phone || 'No contact details'}</small><small>{categories.find(category => category.id === vendor.defaultCategoryId)?.name || 'No default category'}</small></div><div className="customer-data-actions"><button type="button" className="text-link" disabled={saving} onClick={() => setEditing(vendor)}>Edit</button><button type="button" className="text-link" disabled={saving} onClick={() => void deactivate(vendor)}>Deactivate</button></div></article>)
                  : categories.map(category => <article className="customer-data-item" key={category.id}><span className="customer-data-item-icon" style={category.color ? { color: category.color } : undefined}><Tags size={17} /></span><div><strong>{category.name}</strong><small>{category.description || 'Expense category'}</small></div><div className="customer-data-actions"><button type="button" className="text-link" disabled={saving} onClick={() => setEditing(category)}>Edit</button><button type="button" className="text-link" disabled={saving} onClick={() => void deactivate(category)}>Deactivate</button></div></article>)
              ) : <div className="document-empty"><p>No {isVendors ? 'vendors' : 'categories'} yet. Add one to get started.</p></div>}
            </div>
          )}
        </section>
      </div>
    </section>
  )
}
