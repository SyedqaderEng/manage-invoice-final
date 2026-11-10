import { FormEvent, useState } from 'react'
import { ArrowRight, LockKeyhole, Mail } from 'lucide-react'
import { Button } from '../../components/ui/Button'
import { Field } from '../../components/ui/Field'

interface User { id: string; email: string; firstName: string; lastName: string }
interface AuthResponse { accessToken: string; user: User }

interface AuthScreenProps {
  onAuthenticated: (response: AuthResponse) => void
  api: <T>(path: string, options?: RequestInit, token?: string) => Promise<T>
  initialMode: 'login' | 'register'
  onNavigate: (path: string) => void
}

export function AuthScreen({ onAuthenticated, api, initialMode, onNavigate }: AuthScreenProps) {
  const [mode, setMode] = useState<'login' | 'register'>(initialMode)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    setError('')
    setSubmitting(true)
    try {
      const payload = mode === 'register'
        ? { email: String(form.get('email')), password: String(form.get('password')), firstName: String(form.get('firstName')), lastName: String(form.get('lastName')) }
        : { email: String(form.get('email')), password: String(form.get('password')) }
      const response = await api<AuthResponse>(`/api/v1/auth/${mode}`, { method: 'POST', body: JSON.stringify(payload) })
      onAuthenticated(response)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Unable to authenticate')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="auth-layout">
      <section className="auth-brand-panel">
        <button className="auth-brand" type="button" onClick={() => onNavigate('/')}><span className="brand-mark light">L</span><strong>Ledgerly</strong></button>
        <blockquote>Every receipt, right where you need it.</blockquote>
      </section>
      <section className="auth-panel">
        <div className="auth-form-wrap">
          <div className="segmented-control" role="tablist">
            <button className={mode === 'login' ? 'active' : ''} onClick={() => setMode('login')} type="button">Sign in</button>
            <button className={mode === 'register' ? 'active' : ''} onClick={() => setMode('register')} type="button">Create account</button>
          </div>
          <h1>{mode === 'login' ? 'Welcome back' : 'A clearer start.'}</h1>
          <p className="auth-description">{mode === 'login' ? 'Sign in to your personal finance space.' : 'Create your free personal account.'}</p>
          <form className="auth-form" onSubmit={submit}>
            {mode === 'register' ? (
              <div className="two-column-fields">
                <Field label="First name" name="firstName" required />
                <Field label="Last name" name="lastName" required />
              </div>
            ) : null}
            <Field label="Email address" name="email" type="email" autoComplete="email" icon={<Mail size={18} />} required />
            <Field label="Password" name="password" type="password" minLength={8} autoComplete={mode === 'login' ? 'current-password' : 'new-password'} icon={<LockKeyhole size={18} />} required />
            {error ? <p className="form-error" role="alert">{error}</p> : null}
            <Button type="submit" disabled={submitting} icon={<ArrowRight size={18} />}>{submitting ? 'Please wait' : mode === 'login' ? 'Sign in' : 'Create account'}</Button>
          </form>
        </div>
      </section>
    </main>
  )
}
