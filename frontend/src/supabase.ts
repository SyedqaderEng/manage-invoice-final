import { createClient } from '@supabase/supabase-js'

// Supabase Auth handles sign-up, sign-in, and password recovery. The API verifies the access token.
const url = import.meta.env.VITE_SUPABASE_URL
const publishableKey = import.meta.env.VITE_SUPABASE_PUBLISHABLE_KEY
if (!url || !publishableKey) {
  throw new Error('Set VITE_SUPABASE_URL and VITE_SUPABASE_PUBLISHABLE_KEY, or run scripts/start-supabase-web.ps1.')
}

export const supabase = createClient(url, publishableKey, { auth: { flowType: 'pkce' } })
