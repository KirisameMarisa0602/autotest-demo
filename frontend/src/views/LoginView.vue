<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import type { Role } from '../types'

const auth = useAuthStore()
const router = useRouter()

const destinations: Record<Role, string> = {
  TRADER: '/trading',
  APPROVER: '/approvals',
  RISK_MANAGER: '/risk',
}

function login(role: Role) {
  auth.switchRole(role)
  router.push(destinations[role])
}
</script>

<template>
  <section class="panel" aria-labelledby="login-title">
    <h2 id="login-title">选择 Demo 身份</h2>
    <p>这里没有真实登录。按钮只会选择一个固定 Bearer token。</p>
    <div class="actions">
      <button
        data-testid="login-trader"
        type="button"
        aria-label="以交易员身份进入"
        @click="login('TRADER')"
      >
        交易员
      </button>
      <button
        data-testid="login-approver"
        type="button"
        aria-label="以审批员身份进入"
        @click="login('APPROVER')"
      >
        审批员
      </button>
      <button
        data-testid="login-risk-manager"
        type="button"
        aria-label="以风险经理身份进入"
        @click="login('RISK_MANAGER')"
      >
        风险经理
      </button>
    </div>
  </section>
</template>
