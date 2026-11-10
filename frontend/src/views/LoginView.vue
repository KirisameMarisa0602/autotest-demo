<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import type { Role } from '../types'

const auth = useAuthStore()
const router = useRouter()

const destinations: Record<Role, string> = {
  trader: '/trading',
  risk_manager: '/approvals',
  viewer: '/risk',
}

function login(role: Role) {
  auth.switchRole(role)
  router.push(destinations[role])
}
</script>

<template>
  <section class="panel" aria-labelledby="login-title">
    <h2 id="login-title">选择 Demo 身份</h2>
    <p>这里没有真实登录。按钮只会设置 X-Demo-User / X-Demo-Role 请求头。</p>
    <div class="actions">
      <button
        data-testid="login-trader"
        type="button"
        aria-label="以交易员身份进入"
        @click="login('trader')"
      >
        交易员
      </button>
      <button
        data-testid="login-risk-manager"
        type="button"
        aria-label="以风险经理身份进入"
        @click="login('risk_manager')"
      >
        风险经理
      </button>
      <button
        data-testid="login-viewer"
        type="button"
        aria-label="以只读观察者身份进入"
        @click="login('viewer')"
      >
        观察者
      </button>
    </div>
  </section>
</template>
