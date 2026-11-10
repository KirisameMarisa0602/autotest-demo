import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { Role } from '../types'

const TOKENS: Record<Role, string> = {
  TRADER: 'trader-token',
  RISK_MANAGER: 'risk-token',
  APPROVER: 'approver-token',
}

function savedRole(): Role | null {
  const value = localStorage.getItem('demo-role')
  return value === 'TRADER' || value === 'RISK_MANAGER' || value === 'APPROVER'
    ? value
    : null
}

export const useAuthStore = defineStore('auth', () => {
  const role = ref<Role | null>(savedRole())
  const isAuthenticated = computed(() => role.value !== null)

  function switchRole(nextRole: Role) {
    role.value = nextRole
    localStorage.setItem('demo-role', nextRole)
    localStorage.setItem('demo-token', TOKENS[nextRole])
  }

  function logout() {
    role.value = null
    localStorage.removeItem('demo-role')
    localStorage.removeItem('demo-token')
  }

  if (role.value) {
    localStorage.setItem('demo-token', TOKENS[role.value])
  }

  return { role, isAuthenticated, switchRole, logout }
})
