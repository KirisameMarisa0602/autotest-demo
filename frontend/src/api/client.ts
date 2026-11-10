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
  const user = localStorage.getItem('demo-user')
  const role = localStorage.getItem('demo-role')
  if (user) {
    headers.set('X-Demo-User', user)
  }
  if (role) {
    headers.set('X-Demo-Role', role)
  }
  const runId = localStorage.getItem('demo-run-id')
  if (runId) {
    headers.set('X-Run-Id', runId)
  }
  const caseKey = localStorage.getItem('demo-case-key')
  if (caseKey) {
    headers.set('X-Case-Key', caseKey)
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
