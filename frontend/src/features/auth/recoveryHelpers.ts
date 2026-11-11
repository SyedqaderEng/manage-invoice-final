import type { SupabaseClient } from '@supabase/supabase-js'

export function validateNewPassword(password: string, confirmation: string): string | null {
  if (password.length < 8 || password.length > 72) return 'Password must be between 8 and 72 characters.'
  if (new TextEncoder().encode(password).length > 72) return 'Password must be at most 72 UTF-8 bytes.'
  if (password !== confirmation) return 'Passwords do not match.'
  return null
}

export async function requestPasswordRecovery(client: { auth: Pick<SupabaseClient['auth'], 'resetPasswordForEmail'> }, email: string, origin: string) {
  const { error } = await client.auth.resetPasswordForEmail(email.trim(), { redirectTo: `${origin}/reset-password` })
  if (error) throw error
}
