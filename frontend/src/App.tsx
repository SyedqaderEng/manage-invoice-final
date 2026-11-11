import { useEffect, useState } from 'react'
import { ArrowUpRight, FileImage, Plus, Sparkles } from 'lucide-react'
import { AuthScreen } from './features/auth/AuthScreen'
import { PasswordRecovery } from './features/auth/PasswordRecovery'
import { Sidebar } from './components/navigation/Sidebar'
import { TopBar } from './components/navigation/TopBar'
import { Button } from './components/ui/Button'
import { LandingPage } from './features/landing/LandingPage'
import { DocumentLibrary } from './features/documents/DocumentLibrary'
import { SpendingDashboard } from './features/documents/SpendingDashboard'
import { CustomerDataPage } from './features/documents/CustomerDataPage'
import { api, ApiError } from './api'
import { supabase } from './supabase'
import { parseUploadUsage, type UploadUsage } from './features/billing/usage'

interface User { id: string; email: string; firstName: string; lastName: string }

type Api = typeof api

export function App() {
  const [token, setToken] = useState('')
  const [authReady, setAuthReady] = useState(false)
  const [recoveryUserId, setRecoveryUserId] = useState(() => sessionStorage.getItem('ledgerlyRecoveryUserId') || '')
  const [user, setUser] = useState<User | null>(null)
  const [error, setError] = useState('')
  const [sessionState, setSessionState] = useState<'loading' | 'ready' | 'unavailable'>('loading')
  const [sessionRetry, setSessionRetry] = useState(0)
  const [notice, setNotice] = useState('')
  const [mobileOpen, setMobileOpen] = useState(false)
  const [loggingOut, setLoggingOut] = useState(false)
  const [logoutError, setLogoutError] = useState('')
  const [section, setSection] = useState(() => window.location.hash.slice(1) || 'dashboard')
  const [pathname, setPathname] = useState(() => window.location.pathname)

  const navigate = (path: string) => {
    window.history.pushState({}, '', path)
    setPathname(window.location.pathname)
    setSection(window.location.hash.slice(1) || 'dashboard')
  }

  useEffect(() => {
    const { data } = supabase.auth.onAuthStateChange((event, session) => {
      if (event === 'PASSWORD_RECOVERY' && session) {
        sessionStorage.setItem('ledgerlyRecoveryUserId', session.user.id)
        setRecoveryUserId(session.user.id)
        navigate('/reset-password')
      } else if (event === 'SIGNED_OUT' || (session && sessionStorage.getItem('ledgerlyRecoveryUserId') && sessionStorage.getItem('ledgerlyRecoveryUserId') !== session.user.id)) {
        sessionStorage.removeItem('ledgerlyRecoveryUserId')
        setRecoveryUserId('')
      }
      setToken(session?.access_token || '')
      if (!session) setUser(null)
      setAuthReady(true)
    })
    return () => data.subscription.unsubscribe()
  }, [])

  useEffect(() => {
    const onPopState = () => {
      setPathname(window.location.pathname)
      setSection(window.location.hash.slice(1) || 'dashboard')
    }
    window.addEventListener('popstate', onPopState)
    return () => window.removeEventListener('popstate', onPopState)
  }, [])

  useEffect(() => {
    if (!token || recoveryUserId) return
    let active = true
    setSessionState('loading')
    api<User>('/api/v1/auth/me', {}, token)
      .then(account => { if (active) { setUser(account); setSessionState('ready') } })
      .catch(reason => {
        if (!active) return
        if (reason instanceof ApiError && (reason.status === 401 || reason.status === 403)) {
          void supabase.auth.signOut({ scope: 'local' })
          setToken('')
          setUser(null)
          setNotice('Your session has expired. Please sign in again.')
          navigate('/login')
        } else {
          setSessionState('unavailable')
        }
      })
    return () => { active = false }
  }, [token, sessionRetry, recoveryUserId])

  useEffect(() => {
    if (token && !recoveryUserId && ['/', '/login', '/register'].includes(pathname)) navigate('/app')
  }, [token, pathname, recoveryUserId])

  async function logout() {
    if (loggingOut) return
    setLoggingOut(true)
    setLogoutError('')
    try {
      const result = await api<{ tokenRevoked?: boolean }>('/api/v1/auth/logout', { method: 'POST' }, token)
      if (!result.tokenRevoked) throw new Error('The server did not confirm token revocation.')
    } catch (reason) {
      if (!(reason instanceof ApiError && (reason.status === 401 || reason.status === 403))) {
        setLogoutError('Unable to sign out securely. Check your connection and try again.')
        setLoggingOut(false)
        return
      }
    }
    const { error: signOutError } = await supabase.auth.signOut({ scope: 'local' })
    if (signOutError) {
      setLogoutError('Unable to sign out securely. Check your connection and try again.')
      setLoggingOut(false)
      return
    }
    setToken('')
    setUser(null)
    setNotice('')
    setLoggingOut(false)
    navigate('/')
  }

  if (!authReady) return <main className="empty-page-card" aria-live="polite"><p>Loading your account…</p></main>
  if ((pathname === '/forgot-password' || pathname === '/reset-password' || recoveryUserId)) {
    return <PasswordRecovery key={recoveryUserId ? 'reset' : pathname} mode={recoveryUserId || pathname === '/reset-password' ? 'reset' : 'request'}
      recoveryReady={Boolean(token && recoveryUserId)} onNavigate={navigate} onCancel={logout} cancelling={loggingOut} cancelError={logoutError} onComplete={() => {
        sessionStorage.removeItem('ledgerlyRecoveryUserId')
        setRecoveryUserId('')
        setNotice('Your password has been updated.')
        navigate('/app')
      }} />
  }
  if (!token && (pathname === '/login' || pathname === '/register')) {
    return <AuthScreen initialMode={pathname === '/register' ? 'register' : 'login'} onNavigate={navigate} notice={notice} />
  }
  if (!token) return <LandingPage onNavigate={navigate} />

  if (sessionState !== 'ready') {
    return <main className="empty-page-card" aria-live="polite">
      {sessionState === 'loading' ? <p>Loading your account…</p> : <>
        <h1>Unable to load your account</h1>
        <p>Check your connection and try again. Your session has been kept on this device.</p>
        <Button onClick={() => setSessionRetry(value => value + 1)}>Try again</Button>
        <button className="text-link" type="button" disabled={loggingOut} onClick={logout}>{loggingOut ? 'Signing out…' : 'Sign out'}</button>
        {logoutError ? <p className="form-error" role="alert">{logoutError}</p> : null}
      </>}
    </main>
  }

  const userName = user ? `${user.firstName} ${user.lastName}` : 'Your account'
  return (
    <div className="app-shell">
      {mobileOpen ? <button className="sidebar-backdrop" aria-label="Close navigation" onClick={() => setMobileOpen(false)} /> : null}
      <Sidebar
        onLogout={logout}
        loggingOut={loggingOut}
        mobileOpen={mobileOpen}
        onClose={() => setMobileOpen(false)}
        activeSection={section}
        onNavigate={(target) => navigate(`/app#${target}`)}
      />
      <div className="main-column">
        <TopBar userName={userName} onMenu={() => setMobileOpen(true)} />
        <main className="page-content dashboard-content">
          {notice ? <p className="form-success" role="status">{notice}</p> : null}
          {logoutError ? <p className="form-error" role="alert">{logoutError}</p> : null}
          {error ? <p className="form-error" role="alert">{error}</p> : null}
          {section === 'settings' ? (
            <AccountSettings user={user} token={token} api={api} onSaved={setUser} />
          ) : section === 'documents' ? (
            <DocumentLibrary key={user?.id} token={token} api={api} />
          ) : section === 'vendors' || section === 'categories' ? (
            <CustomerDataPage section={section} token={token} api={api} />
          ) : (
            <Dashboard key={user?.id} user={user} token={token} onNavigate={navigate} />
          )}
        </main>
      </div>
    </div>
  )
}

function Dashboard({ user, token, onNavigate }: { user: User | null; token: string; onNavigate: (path: string) => void }) {
  const firstName = user?.firstName || 'there'
  const [usage, setUsage] = useState<UploadUsage | null>(null)
  const [usageError, setUsageError] = useState(false)
  const [usageRetry, setUsageRetry] = useState(0)
  useEffect(() => {
    let active = true
    setUsage(null)
    setUsageError(false)
    api<unknown>('/api/v1/billing/personal/usage', {}, token)
      .then(parseUploadUsage)
      .then(result => { if (active) setUsage(result) })
      .catch(() => { if (active) setUsageError(true) })
    return () => { active = false }
  }, [token, usageRetry])
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
      <section className="empty-data-card" aria-label="Document upload tracking">
        <h2>Document uploads</h2>
        {usageError ? <><p role="alert">Unable to load receipt usage.</p><Button variant="secondary" onClick={() => setUsageRetry(value => value + 1)}>Retry</Button></> :
          !usage ? <p role="status">Loading receipt usage…</p> : <>
            <p>{usage.historyComplete ? 'Tracked uploads' : 'At least'}: <strong>{usage.trackedUploads}</strong></p>
            {!usage.historyComplete ? <p>Earlier deleted documents may not be included in this count.</p> : null}
            <p>This counts document uploads, not Free-plan receipt usage. {usage.limitEnforced ? 'A receipt limit is enforced.' : 'The planned Free limit is not yet enforced.'}</p>
          </>}
      </section>
      <SpendingDashboard token={token} />
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
