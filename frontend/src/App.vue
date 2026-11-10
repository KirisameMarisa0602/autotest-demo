<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'
import type { Role } from './types'

const auth = useAuthStore()
const router = useRouter()

function changeRole(event: Event) {
  auth.switchRole((event.target as HTMLSelectElement).value as Role)
}

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <header class="app-header">
    <div>
      <h1>衍生品交易 Demo</h1>
      <p>仅用于自动化测试与教学，非生产系统</p>
    </div>
    <div v-if="auth.isAuthenticated" class="identity">
      <label for="global-role">当前身份</label>
      <select
        id="global-role"
        :value="auth.role ?? ''"
        data-testid="global-role-switcher"
        @change="changeRole"
      >
        <option value="trader">trader</option>
        <option value="risk_manager">risk_manager</option>
        <option value="viewer">viewer</option>
      </select>
      <button data-testid="logout-button" type="button" @click="logout">退出</button>
    </div>
  </header>

  <nav v-if="auth.isAuthenticated" aria-label="主导航">
    <RouterLink data-testid="nav-trading" to="/trading">交易台</RouterLink>
    <RouterLink data-testid="nav-approvals" to="/approvals">审批台</RouterLink>
    <RouterLink data-testid="nav-risk" to="/risk">风险看板</RouterLink>
    <RouterLink data-testid="nav-audit" to="/audit">审计时间线</RouterLink>
  </nav>

  <main>
    <RouterView />
  </main>
</template>
