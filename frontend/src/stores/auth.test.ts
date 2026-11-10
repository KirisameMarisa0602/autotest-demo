import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from './auth'

describe('auth store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('maps a selected role to its fixed demo token and clears it on logout', () => {
    const store = useAuthStore()

    store.switchRole('RISK_MANAGER')

    expect(store.role).toBe('RISK_MANAGER')
    expect(store.isAuthenticated).toBe(true)
    expect(localStorage.getItem('demo-token')).toBe('risk-token')

    store.logout()
    expect(store.isAuthenticated).toBe(false)
    expect(localStorage.getItem('demo-token')).toBeNull()
  })
})
