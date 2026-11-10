<script setup lang="ts">
import { ref } from 'vue'
import { ApiClientError, apiFetch } from '../api/client'
import type { RiskReport } from '../types'

const portfolioCode = ref('ALPHA')
const report = ref<RiskReport | null>(null)
const error = ref('')

async function loadRisk() {
  error.value = ''
  try {
    report.value = await apiFetch<RiskReport>(
      `/risk/portfolios/${encodeURIComponent(portfolioCode.value)}`,
    )
  } catch (reason) {
    error.value = reason instanceof ApiClientError
      ? `${reason.body.code}: ${reason.message}`
      : String(reason)
  }
}
</script>

<template>
  <section aria-labelledby="risk-title">
    <h2 id="risk-title">风险看板</h2>
    <div class="panel actions">
      <label for="risk-portfolio">组合</label>
      <select id="risk-portfolio" v-model="portfolioCode" data-testid="risk-portfolio">
        <option value="ALPHA">ALPHA</option>
        <option value="BETA">BETA</option>
      </select>
      <button type="button" data-testid="load-risk" @click="loadRisk">计算风险</button>
    </div>
    <p v-if="error" class="error" role="alert" data-testid="risk-error">{{ error }}</p>

    <div v-if="report" class="panel" data-testid="risk-report">
      <dl>
        <dt>已批准交易数</dt>
        <dd data-testid="approved-trade-count">{{ report.approvedTradeCount }}</dd>
        <dt>PV ({{ report.currency }})</dt>
        <dd data-testid="risk-pv">{{ report.presentValue }}</dd>
        <dt>Delta</dt>
        <dd data-testid="risk-delta">{{ report.delta }}</dd>
      </dl>

      <h3>压力场景</h3>
      <table>
        <thead>
          <tr>
            <th>场景</th>
            <th>Spot shock</th>
            <th>压力 PV</th>
            <th>P&amp;L</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="scenario in report.scenarios" :key="scenario.key" data-testid="risk-scenario">
            <td>{{ scenario.key }}</td>
            <td>{{ scenario.spotShockPct }}</td>
            <td>{{ scenario.stressedPresentValue }}</td>
            <td>{{ scenario.pnl }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
