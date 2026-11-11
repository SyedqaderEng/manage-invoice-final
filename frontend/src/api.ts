export class ApiError extends Error {
  constructor(message: string, readonly status: number) {
    super(message)
  }
}

export class ApiNetworkError extends Error {
  constructor(cause: TypeError) {
    super('Cannot reach the application server. Check your connection and make sure the app is running.', { cause })
  }
}

export async function api<T>(path: string, options: RequestInit = {}, token?: string): Promise<T> {
  const headers = new Headers(options.headers)
  if (!headers.has('Content-Type') && !(options.body instanceof FormData)) headers.set('Content-Type', 'application/json')
  if (token) headers.set('Authorization', `Bearer ${token}`)
  let response: Response
  try {
    response = await fetch(path, { ...options, headers })
  } catch (reason) {
    if (reason instanceof TypeError) throw new ApiNetworkError(reason)
    throw reason
  }
  if (!response.ok) {
    const body = await response.json().catch(() => null)
    throw new ApiError(body?.detail || body?.message || 'Request failed', response.status)
  }
  return response.status === 204 ? undefined as T : response.json() as Promise<T>
}
