import { afterEach, describe, expect, it, vi } from 'vitest'
import { api, ApiError, ApiNetworkError } from './api'

afterEach(() => vi.unstubAllGlobals())

describe('api', () => {
  it('retains the response status and backend message', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(
      JSON.stringify({ message: 'An account with this email already exists' }),
      { status: 409, headers: { 'Content-Type': 'application/json' } },
    )))
    await expect(api('/api/v1/auth/register')).rejects.toMatchObject({
      message: 'An account with this email already exists', status: 409,
    } satisfies Partial<ApiError>)
  })

  it('leaves network errors distinguishable from authentication failures', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Network unavailable')))
    await expect(api('/api/v1/auth/me')).rejects.toBeInstanceOf(ApiNetworkError)
    await expect(api('/api/v1/auth/me')).rejects.toThrow('Cannot reach the application server')
  })
})
