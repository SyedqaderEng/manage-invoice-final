import { FormEvent, useState } from 'react'
import { ArrowRight, LockKeyhole, Mail } from 'lucide-react'
import { Button } from '../../components/ui/Button'
import { Field } from '../../components/ui/Field'
import { supabase } from '../../supabase'

interface AuthScreenProps {
  initialMode: 'login' | 'register'
  onNavigate: (path: string) => void
  notice?: string
}

export function AuthScreen({ initialMode, onNavigate, notice }: AuthScreenProps) {
  const [mode, setMode] = useState<'login' | 'register'>(initialMode)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [success, setSuccess] = useState('')

  const signInWithGoogle = async () => {
    if (submitting) return
    setError('')
    setSuccess('')
    setSubmitting(true)
    try {
      const { error: authError } = await supabase.auth.signInWithOAuth({
        provider: 'google',
        options: { redirectTo: `${window.location.origin}/app` },
      })
      if (authError) throw authError
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Unable to sign in with Google.')
      setSubmitting(false)
    }
  }

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (submitting) return
    const form = new FormData(event.currentTarget)
    setError('')
    setSuccess('')
    if (mode === 'register' && form.get('password') !== form.get('confirmPassword')) {
      setError('Passwords do not match.')
      return
    }
    setSubmitting(true)
    try {
      const email = String(form.get('email'))
      const password = String(form.get('password'))
      // The session is picked up by the onAuthStateChange listener in App.
      if (mode === 'register') {
        const { data, error: authError } = await supabase.auth.signUp({
          email,
          password,
          options: {
            data: { first_name: String(form.get('firstName')).trim(), last_name: String(form.get('lastName')).trim() },
            emailRedirectTo: `${window.location.origin}/login`,
          },
        })
        if (authError) throw authError
        if (!data.session) {
          setSuccess('If this address can be registered, check your email for a confirmation link.')
        }
      } else {
        const { error: authError } = await supabase.auth.signInWithPassword({ email, password })
        if (authError) throw authError
      }
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
            <button className={mode === 'login' ? 'active' : ''} onClick={() => { setError(''); setSuccess(''); setMode('login') }} disabled={submitting} type="button">Sign in</button>
            <button className={mode === 'register' ? 'active' : ''} onClick={() => { setError(''); setSuccess(''); setMode('register') }} disabled={submitting} type="button">Create account</button>
          </div>
          <h1>{mode === 'login' ? 'Welcome back' : 'A clearer start.'}</h1>
          <p className="auth-description">{mode === 'login' ? 'Sign in to your personal finance space.' : 'Create your free personal account.'}</p>
          <form className="auth-form" onSubmit={submit}>
            {mode === 'register' ? (
              <div className="two-column-fields">
                <Field label="First name" name="firstName" maxLength={100} required />
                <Field label="Last name" name="lastName" maxLength={100} required />
              </div>
            ) : null}
            <Field label="Email address" name="email" type="email" maxLength={320} autoComplete="email" icon={<Mail size={18} />} required />
            <Field label="Password" name="password" type="password" minLength={mode === 'register' ? 8 : undefined} maxLength={mode === 'register' ? 72 : undefined} autoComplete={mode === 'login' ? 'current-password' : 'new-password'} icon={<LockKeyhole size={18} />} required />
            {mode === 'register' ? <Field label="Confirm password" name="confirmPassword" type="password" autoComplete="new-password" required /> : null}
            {notice ? <p className="form-success" role="status">{notice}</p> : null}
            {success ? <p className="form-success" role="status">{success}</p> : null}
            {error ? <p className="form-error" role="alert">{error}</p> : null}
            <Button type="submit" disabled={submitting} icon={<ArrowRight size={18} />}>{submitting ? 'Please wait' : mode === 'login' ? 'Sign in' : 'Create account'}</Button>
            <Button type="button" variant="secondary" disabled={submitting} onClick={signInWithGoogle}>Continue with Google</Button>
            {mode === 'login' ? <button className="text-link" type="button" disabled={submitting} onClick={() => onNavigate('/forgot-password')}>Forgot password?</button> : null}
          </form>
        </div>
      </section>
    </main>
  )
}
