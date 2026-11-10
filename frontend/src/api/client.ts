const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api'

export interface ApiErrorBody {
  status: number
  code: string
  message: string
  fieldErrors?: Array<{ field: string; message: string }>
}

export class ApiClientError extends Error {
  constructor(public readonly body: ApiErrorBody) {
    super(body.message)
  }
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const headers = new Headers(options.headers)
  const token = localStorage.getItem('demo-token')
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers })
  if (!response.ok) {
    const fallback: ApiErrorBody = {
      status: response.status,
      code: 'HTTP_ERROR',
      message: `HTTP ${response.status}`,
    }
    const body = await response.json().catch(() => fallback) as ApiErrorBody
    throw new ApiClientError(body)
  }
  return response.json() as Promise<T>
}
