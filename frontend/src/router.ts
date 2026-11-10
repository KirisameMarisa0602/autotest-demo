import { createRouter, createWebHistory } from 'vue-router'
import LoginView from './views/LoginView.vue'
import TradingDeskView from './views/TradingDeskView.vue'
import ApprovalDeskView from './views/ApprovalDeskView.vue'
import RiskDashboardView from './views/RiskDashboardView.vue'
import AuditTimelineView from './views/AuditTimelineView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/login' },
    { path: '/login', name: 'login', component: LoginView },
    { path: '/trading', name: 'trading', component: TradingDeskView },
    { path: '/approvals', name: 'approvals', component: ApprovalDeskView },
    { path: '/risk', name: 'risk', component: RiskDashboardView },
    { path: '/audit', name: 'audit', component: AuditTimelineView },
  ],
})

router.beforeEach((to) => {
  if (to.name !== 'login' && !localStorage.getItem('demo-token')) {
    return { name: 'login' }
  }
})

export default router
