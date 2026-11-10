import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from './auth'

describe('auth store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('maps a selected role to its fixed demo user and clears it on logout', () => {
    const store = useAuthStore()

    store.switchRole('risk_manager')

    expect(store.role).toBe('risk_manager')
    expect(store.user).toBe('bob')
    expect(store.isAuthenticated).toBe(true)
    expect(localStorage.getItem('demo-user')).toBe('bob')

    store.logout()
    expect(store.isAuthenticated).toBe(false)
    expect(localStorage.getItem('demo-user')).toBeNull()
  })
})
