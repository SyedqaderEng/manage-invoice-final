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
      await api(isVendors ? '/api/v1/vendors' : '/api/v1/categories', {
        method: 'POST',
        body: JSON.stringify(payload),
      }, token)
      formElement.reset()
      setMessage(isVendors ? 'Vendor added.' : 'Category added.')
      await refresh()
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : `Unable to add ${isVendors ? 'vendor' : 'category'}`)
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
        <form className="customer-data-form" onSubmit={create}>
          <div className="customer-data-form-title">{isVendors ? <Store size={18} /> : <Tags size={18} />}<strong>Add {isVendors ? 'a vendor' : 'a category'}</strong></div>
          {isVendors ? (
            <>
              <label className="field">Vendor name<input name="displayName" required maxLength={255} /></label>
              <label className="field">Email (optional)<input name="email" type="email" maxLength={320} /></label>
              <label className="field">Phone (optional)<input name="phone" maxLength={50} /></label>
              <label className="field">Address (optional)<textarea name="address" maxLength={2000} rows={3} /></label>
              <label className="field">Default category (optional)<select name="defaultCategoryId" defaultValue=""><option value="">No default</option>{categories.map(category => <option value={category.id} key={category.id}>{category.name}</option>)}</select></label>
            </>
          ) : (
            <>
              <label className="field">Category name<input name="name" required maxLength={100} /></label>
              <label className="field">Description (optional)<textarea name="description" maxLength={2000} rows={3} /></label>
              <label className="field">Color (optional)<input name="color" type="color" defaultValue="#6375d5" /></label>
            </>
          )}
          {error ? <p className="form-error" role="alert">{error}</p> : null}
          {message ? <p className="form-success" role="status">{message}</p> : null}
          <Button type="submit" disabled={saving}>{saving ? 'Saving…' : `Add ${isVendors ? 'vendor' : 'category'}`}</Button>
        </form>

        <section className="customer-data-list" aria-label={isVendors ? 'Your vendors' : 'Your categories'}>
          <h2>{isVendors ? 'Your vendors' : 'Your categories'}</h2>
          {loading ? <p className="document-empty">Loading…</p> : (
            <div className="customer-data-items">
              {(isVendors ? vendors : categories).length ? (
                isVendors
                  ? vendors.map(vendor => <article className="customer-data-item" key={vendor.id}><span className="customer-data-item-icon"><Store size={17} /></span><div><strong>{vendor.displayName}</strong><small>{vendor.email || vendor.phone || 'No contact details'}</small><small>{categories.find(category => category.id === vendor.defaultCategoryId)?.name || 'No default category'}</small></div></article>)
                  : categories.map(category => <article className="customer-data-item" key={category.id}><span className="customer-data-item-icon" style={category.color ? { color: category.color } : undefined}><Tags size={17} /></span><div><strong>{category.name}</strong><small>{category.description || 'Expense category'}</small></div></article>)
              ) : <div className="document-empty"><p>No {isVendors ? 'vendors' : 'categories'} yet. Add one to get started.</p></div>}
            </div>
          )}
        </section>
      </div>
    </section>
  )
}
