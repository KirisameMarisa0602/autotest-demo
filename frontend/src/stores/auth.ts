import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { Role } from '../types'

const USERS: Record<Role, string> = {
  trader: 'alice',
  risk_manager: 'bob',
  viewer: 'carol',
}

function savedRole(): Role | null {
  const value = localStorage.getItem('demo-role')
  return value === 'trader' || value === 'risk_manager' || value === 'viewer'
    ? value
    : null
}

export const useAuthStore = defineStore('auth', () => {
  const role = ref<Role | null>(savedRole())
  const user = computed(() => (role.value ? USERS[role.value] : null))
  const isAuthenticated = computed(() => role.value !== null)

  function switchRole(nextRole: Role) {
    role.value = nextRole
    localStorage.setItem('demo-role', nextRole)
    localStorage.setItem('demo-user', USERS[nextRole])
  }

  function logout() {
    role.value = null
    localStorage.removeItem('demo-role')
    localStorage.removeItem('demo-user')
  }

  if (role.value) {
    localStorage.setItem('demo-user', USERS[role.value])
  }

  return { role, user, isAuthenticated, switchRole, logout }
})
