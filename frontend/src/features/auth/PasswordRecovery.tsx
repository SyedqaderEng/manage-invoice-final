import { FormEvent, useState } from 'react'
import { Button } from '../../components/ui/Button'
import { Field } from '../../components/ui/Field'
import { supabase } from '../../supabase'
import { requestPasswordRecovery, validateNewPassword } from './recoveryHelpers'

interface RecoveryProps {
  mode: 'request' | 'reset'
  recoveryReady: boolean
  onNavigate: (path: string) => void
  onComplete: () => void
  onCancel: () => void
  cancelError: string
  cancelling: boolean
}

export function PasswordRecovery({ mode, recoveryReady, onNavigate, onComplete, onCancel, cancelError, cancelling }: RecoveryProps) {
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (submitting) return
    const form = new FormData(event.currentTarget)
    setError('')
    setSuccess('')
    if (mode === 'reset') {
      const validation = validateNewPassword(String(form.get('password')), String(form.get('confirmation')))
      if (validation) { setError(validation); return }
      if (!recoveryReady) { setError('This recovery link is invalid or has expired. Request a new link.'); return }
    }
    setSubmitting(true)
    try {
      if (mode === 'request') {
        await requestPasswordRecovery(supabase, String(form.get('email')), window.location.origin)
        setSuccess('If an account exists for that address, a recovery email will be sent.')
      } else {
        const { error: updateError } = await supabase.auth.updateUser({ password: String(form.get('password')) })
        if (updateError) throw updateError
        onComplete()
      }
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'The request could not be completed. Try again.')
    } finally {
      setSubmitting(false)
    }
  }

  return <main className="auth-layout">
    <section className="auth-brand-panel">
      <button className="auth-brand" type="button" onClick={() => onNavigate('/')}><span className="brand-mark light">L</span><strong>Ledgerly</strong></button>
      <blockquote>Every receipt, right where you need it.</blockquote>
    </section>
    <section className="auth-panel"><div className="auth-form-wrap">
      <h1>{mode === 'request' ? 'Reset your password' : 'Choose a new password'}</h1>
      <p className="auth-description">{mode === 'request' ? 'Enter your email and we’ll send a recovery link if an account exists.' : 'Enter a new password for your account.'}</p>
      {mode === 'reset' && !recoveryReady
        ? <p className="form-error" role="alert">This recovery link is invalid or has expired. Request a new link.</p>
        : <form className="auth-form" onSubmit={submit}>
          {mode === 'request'
            ? <Field label="Email address" name="email" type="email" maxLength={320} autoComplete="email" required />
            : <>
              <Field label="New password" name="password" type="password" minLength={8} maxLength={72} autoComplete="new-password" required />
              <Field label="Confirm new password" name="confirmation" type="password" minLength={8} maxLength={72} autoComplete="new-password" required />
            </>}
          {success ? <p className="form-success" role="status">{success}</p> : null}
          {error ? <p className="form-error" role="alert">{error}</p> : null}
          <Button type="submit" disabled={submitting || cancelling}>{submitting ? 'Please wait' : mode === 'request' ? 'Send recovery link' : 'Update password'}</Button>
        </form>}
      {cancelError ? <p className="form-error" role="alert">{cancelError}</p> : null}
      <button className="text-link" type="button" disabled={submitting || cancelling} onClick={() => recoveryReady ? onCancel() : onNavigate(mode === 'reset' ? '/forgot-password' : '/login')}>
        {cancelling ? 'Signing out…' : recoveryReady ? 'Cancel and sign out' : mode === 'reset' ? 'Request a new link' : 'Back to sign in'}
      </button>
    </div></section>
  </main>
}
