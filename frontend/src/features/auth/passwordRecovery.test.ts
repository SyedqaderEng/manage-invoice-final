import { describe, expect, it, vi } from 'vitest'
import { requestPasswordRecovery, validateNewPassword } from './recoveryHelpers'

describe('password recovery', () => {
  it('requires matching passwords within character and bcrypt byte limits', () => {
    expect(validateNewPassword('1234567', '1234567')).toContain('8 and 72')
    expect(validateNewPassword('12345678', 'different')).toBe('Passwords do not match.')
    expect(validateNewPassword('é'.repeat(37), 'é'.repeat(37))).toContain('72 UTF-8 bytes')
    expect(validateNewPassword('Password123', 'Password123')).toBeNull()
  })

  it('requests a recovery link with an exact same-origin return path', async () => {
    const resetPasswordForEmail = vi.fn().mockResolvedValue({ error: null })
    const client = { auth: { resetPasswordForEmail } }
    await requestPasswordRecovery(client, ' person@example.com ', 'https://ledgerly.example')
    expect(resetPasswordForEmail).toHaveBeenCalledWith('person@example.com', {
      redirectTo: 'https://ledgerly.example/reset-password',
    })
  })

  it('surfaces provider errors instead of claiming email delivery', async () => {
    const error = new Error('Email service unavailable')
    const client = { auth: { resetPasswordForEmail: vi.fn().mockResolvedValue({ error }) } }
    await expect(requestPasswordRecovery(client, 'person@example.com', 'https://ledgerly.example')).rejects.toThrow(error)
  })
})
