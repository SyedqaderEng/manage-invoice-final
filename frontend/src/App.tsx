import { useEffect, useState } from 'react'
import { ArrowUpRight, FileImage, FileText, Plus, Search, Sparkles } from 'lucide-react'
import { AuthScreen } from './features/auth/AuthScreen'
import { Sidebar } from './components/navigation/Sidebar'
import { TopBar } from './components/navigation/TopBar'
import { Button } from './components/ui/Button'
import { LandingPage } from './features/landing/LandingPage'
import { DocumentLibrary } from './features/documents/DocumentLibrary'
import { CustomerDataPage } from './features/documents/CustomerDataPage'

interface User { id: string; email: string; firstName: string; lastName: string }
interface AuthResponse { accessToken: string; user: User }

const api = async <T,>(path: string, options: RequestInit = {}, token?: string): Promise<T> => {
  const headers = new Headers(options.headers)
  if (!headers.has('Content-Type') && !(options.body instanceof FormData)) headers.set('Content-Type', 'application/json')
  if (token) headers.set('Authorization', `Bearer ${token}`)
  const response = await fetch(path, { ...options, headers })
  if (!response.ok) {
    const body = await response.json().catch(() => null)
    throw new Error(body?.detail || body?.message || 'Request failed')
  }
  return response.status === 204 ? undefined as T : response.json() as Promise<T>
}
type Api = typeof api

export function App() {
  const [token, setToken] = useState(() => localStorage.getItem('ledgerlyToken') || '')
  const [user, setUser] = useState<User | null>(null)
  const [error, setError] = useState('')
  const [mobileOpen, setMobileOpen] = useState(false)
  const [section, setSection] = useState(() => window.location.hash.slice(1) || 'dashboard')
  const [pathname, setPathname] = useState(() => window.location.pathname)

  const navigate = (path: string) => {
    window.history.pushState({}, '', path)
    setPathname(window.location.pathname)
    setSection(window.location.hash.slice(1) || 'dashboard')
  }

  useEffect(() => {
    const onPopState = () => {
      setPathname(window.location.pathname)
      setSection(window.location.hash.slice(1) || 'dashboard')
    }
    window.addEventListener('popstate', onPopState)
    return () => window.removeEventListener('popstate', onPopState)
  }, [])

  useEffect(() => {
    if (!token) return
    api<User>('/api/v1/auth/me', {}, token)
      .then(setUser)
      .catch(() => logout())
  }, [token])

  useEffect(() => {
    if (token && ['/', '/login', '/register'].includes(pathname)) navigate('/app')
  }, [token, pathname])

  const authenticated = (response: AuthResponse) => {
    localStorage.setItem('ledgerlyToken', response.accessToken)
    setToken(response.accessToken)
    setUser(response.user)
    navigate('/app')
  }

  function logout() {
    localStorage.removeItem('ledgerlyToken')
    setToken('')
    setUser(null)
    navigate('/')
  }

  if (!token && (pathname === '/login' || pathname === '/register')) {
    return <AuthScreen api={api} onAuthenticated={authenticated} initialMode={pathname === '/register' ? 'register' : 'login'} onNavigate={navigate} />
  }
  if (!token) return <LandingPage onNavigate={navigate} />

  const userName = user ? `${user.firstName} ${user.lastName}` : 'Your account'
  return (
    <div className="app-shell">
      {mobileOpen ? <button className="sidebar-backdrop" aria-label="Close navigation" onClick={() => setMobileOpen(false)} /> : null}
      <Sidebar
        onLogout={logout}
        mobileOpen={mobileOpen}
        onClose={() => setMobileOpen(false)}
        activeSection={section}
        onNavigate={(target) => navigate(`/app#${target}`)}
      />
      <div className="main-column">
        <TopBar userName={userName} onMenu={() => setMobileOpen(true)} />
        <main className="page-content dashboard-content">
          {error ? <p className="form-error" role="alert">{error}</p> : null}
          {section === 'settings' ? (
            <AccountSettings user={user} token={token} api={api} onSaved={setUser} />
          ) : section === 'documents' ? (
            <DocumentLibrary token={token} api={api} />
          ) : section === 'vendors' || section === 'categories' ? (
            <CustomerDataPage section={section} token={token} api={api} />
          ) : (
            <Dashboard user={user} onNavigate={navigate} />
          )}
        </main>
      </div>
    </div>
  )
}

function Dashboard({ user, onNavigate }: { user: User | null; onNavigate: (path: string) => void }) {
  const firstName = user?.firstName || 'there'
  return (
    <>
      <div className="dashboard-heading">
        <div><p className="section-kicker">YOUR PERSONAL FINANCE SPACE</p><h1>A clearer picture, {firstName}.</h1><p className="dashboard-subtitle">Your receipts and invoices, organized around your life.</p></div>
        <Button onClick={() => onNavigate('/app#documents')}><Plus size={17} /> Upload documents</Button>
      </div>
      <section className="dashboard-welcome">
        <div className="welcome-copy"><span className="welcome-label"><Sparkles size={14} /> A FRESH START</span><h2>Make your paperwork<br />work a little less.</h2><p>Bring your receipts and invoices together. Keep the original close, find what you need, and see your spending more clearly.</p><Button onClick={() => onNavigate('/app#documents')}>Upload a document <ArrowUpRight size={16} /></Button></div>
        <div className="welcome-art" aria-hidden="true"><div className="art-glow" /><div className="art-sheet sheet-back"><span /><span /><span /></div><div className="art-sheet sheet-front"><span className="art-receipt-icon"><FileImage size={20} /></span><span className="art-line" /><span className="art-line short" /><span className="art-line" /><span className="art-total"><i /> <b /></span></div><div className="art-sparkle sparkle-a">✳</div><div className="art-sparkle sparkle-b">✦</div><div className="art-sticker">YOUR<br />RECORDS,<br /><strong>IN FOCUS</strong></div></div>
      </section>
      <section className="dashboard-section-heading"><div><p className="section-kicker">WHEN YOU’RE READY</p><h2>Your overview will grow with you.</h2></div><button className="text-link" onClick={() => onNavigate('/app#documents')}>Your documents <ArrowUpRight size={15} /></button></section>
      <section className="dashboard-empty-grid">
        <article className="empty-data-card"><div className="empty-data-top"><span className="small-icon blue"><Search size={17} /></span><span>SEARCH YOUR RECORDS</span></div><div className="empty-data-illustration"><div className="search-orb"><Search size={24} /></div><i /><i /><i /></div><h3>Find it when you need it.</h3><p>Your documents will be searchable by vendor, item, date, and amount.</p></article>
        <article className="empty-data-card"><div className="empty-data-top"><span className="small-icon violet"><Sparkles size={17} /></span><span>SEE THE BIGGER PICTURE</span></div><div className="empty-data-illustration chart-illustration"><i /><i /><i /><i /><i /><i /><i /></div><h3>Understand where it goes.</h3><p>Once you have records, your spending trends and categories will take shape here.</p></article>
        <article className="empty-data-card"><div className="empty-data-top"><span className="small-icon peach"><FileText size={17} /></span><span>KEEP THE DETAILS</span></div><div className="empty-data-illustration document-illustration"><span><FileText size={20} /></span><i /><i /><b /></div><h3>Originals stay connected.</h3><p>Keep source documents close to the details you’ve reviewed and saved.</p></article>
      </section>
      <p className="dashboard-privacy"><span><Sparkles size={14} /></span> A personal space, just for your records. Your information stays yours.</p>
    </>
  )
}

function AccountSettings({ user, token, api, onSaved }: {
  user: User | null
  token: string
  api: Api
  onSaved: (user: User) => void
}) {
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const save = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    setError('')
    setMessage('')
    try {
      const updated = await api<User>('/api/v1/auth/me', {
        method: 'PATCH',
        body: JSON.stringify({ firstName: form.get('firstName'), lastName: form.get('lastName') }),
      }, token)
      onSaved(updated)
      setMessage('Your profile has been saved.')
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Unable to save settings')
    }
  }
  return (
    <section className="settings-card">
      <div className="page-heading"><div><p className="section-kicker">YOUR ACCOUNT</p><h1>Profile settings</h1></div></div>
      <form className="settings-form" onSubmit={save}>
        <label className="field">Email address<input value={user?.email || ''} readOnly /></label>
        <label className="field">First name<input name="firstName" defaultValue={user?.firstName || ''} required /></label>
        <label className="field">Last name<input name="lastName" defaultValue={user?.lastName || ''} required /></label>
        {error ? <p className="form-error" role="alert">{error}</p> : null}
        {message ? <p className="form-success" role="status">{message}</p> : null}
        <Button type="submit">Save changes</Button>
      </form>
    </section>
  )
}
