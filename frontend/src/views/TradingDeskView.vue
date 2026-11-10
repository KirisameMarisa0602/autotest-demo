<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ApiClientError } from '../api/client'
import { useTradesStore } from '../stores/trades'
import type { CreateTrade } from '../types'

const store = useTradesStore()
const error = ref('')
const message = ref('')
const statusFilter = ref('')
const idempotencyKey = ref(`ui-${Date.now()}`)
const form = reactive<CreateTrade>({
  portfolioCode: 'ALPHA',
  tradeType: 'EUROPEAN_OPTION',
  side: 'LONG',
  underlying: 'AAPL',
  quantity: 10,
  strike: 100,
  maturity: '2027-12-31',
  optionType: 'CALL',
})

onMounted(refresh)

async function refresh() {
  error.value = ''
  try {
    await store.load(form.portfolioCode, statusFilter.value)
  } catch (reason) {
    showError(reason)
  }
}

async function createTrade() {
  error.value = ''
  message.value = ''
  const request: CreateTrade = {
    portfolioCode: form.portfolioCode,
    tradeType: form.tradeType,
    side: form.side,
    underlying: form.underlying,
    quantity: form.quantity,
    strike: form.strike,
    maturity: form.maturity,
    ...(form.tradeType === 'EUROPEAN_OPTION' ? { optionType: form.optionType } : {}),
  }
  try {
    const trade = await store.create(request, idempotencyKey.value)
    message.value = `已创建草稿 ${trade.id}`
    idempotencyKey.value = `ui-${Date.now()}`
  } catch (reason) {
    showError(reason)
  }
}

async function submitTrade(index: number) {
  error.value = ''
  try {
    const trade = await store.submit(store.trades[index])
    message.value = `已提交 ${trade.id}`
  } catch (reason) {
    showError(reason)
  }
}

function showError(reason: unknown) {
  error.value = reason instanceof ApiClientError ? `${reason.body.code}: ${reason.message}` : String(reason)
}
</script>

<template>
  <section aria-labelledby="trading-title">
    <h2 id="trading-title">交易台</h2>
    <form class="panel" data-testid="trade-form" @submit.prevent="createTrade">
      <div class="grid">
        <label for="portfolio">组合</label>
        <select id="portfolio" v-model="form.portfolioCode" data-testid="trade-portfolio">
          <option value="ALPHA">ALPHA</option>
          <option value="BETA">BETA</option>
        </select>

        <label for="trade-type">产品类型</label>
        <select id="trade-type" v-model="form.tradeType" data-testid="trade-type">
          <option value="EUROPEAN_OPTION">European option</option>
          <option value="FORWARD">Forward</option>
        </select>

        <label for="side">方向</label>
        <select id="side" v-model="form.side" data-testid="trade-side">
          <option value="LONG">LONG</option>
          <option value="SHORT">SHORT</option>
        </select>

        <label for="underlying">标的</label>
        <select id="underlying" v-model="form.underlying" data-testid="trade-underlying">
          <option value="AAPL">AAPL</option>
          <option value="MSFT">MSFT</option>
        </select>

        <label for="quantity">数量</label>
        <input
          id="quantity"
          v-model.number="form.quantity"
          data-testid="trade-quantity"
          type="number"
          min="0.000001"
          step="any"
          required
        />

        <label for="strike">执行价 / 交割价</label>
        <input
          id="strike"
          v-model.number="form.strike"
          data-testid="trade-strike"
          type="number"
          min="0.000001"
          step="any"
          required
        />

        <label for="maturity">到期日</label>
        <input
          id="maturity"
          v-model="form.maturity"
          data-testid="trade-maturity"
          type="date"
          required
        />

        <template v-if="form.tradeType === 'EUROPEAN_OPTION'">
          <label for="option-type">期权类型</label>
          <select id="option-type" v-model="form.optionType" data-testid="trade-option-type">
            <option value="CALL">CALL</option>
            <option value="PUT">PUT</option>
          </select>
        </template>

        <label for="idempotency-key">幂等键</label>
        <input
          id="idempotency-key"
          v-model="idempotencyKey"
          data-testid="idempotency-key"
          maxlength="100"
          required
        />
      </div>
      <div class="actions">
        <button data-testid="create-trade" type="submit">创建草稿</button>
        <button class="secondary" type="button" data-testid="refresh-trades" @click="refresh">
          刷新
        </button>
        <label for="status-filter">状态筛选</label>
        <select
          id="status-filter"
          v-model="statusFilter"
          data-testid="trade-status-filter"
          @change="refresh"
        >
          <option value="">全部</option>
          <option value="DRAFT">DRAFT</option>
          <option value="SUBMITTED">SUBMITTED</option>
          <option value="APPROVED">APPROVED</option>
          <option value="REJECTED">REJECTED</option>
        </select>
      </div>
    </form>

    <p v-if="error" class="error" role="alert" data-testid="trade-error">{{ error }}</p>
    <p v-if="message" class="success" role="status" data-testid="trade-message">{{ message }}</p>
    <p data-testid="trade-total">共 {{ store.totalItems }} 笔</p>

    <div class="panel">
      <h3>交易</h3>
      <table>
        <thead>
          <tr>
            <th>产品</th>
            <th>方向</th>
            <th>标的</th>
            <th>数量</th>
            <th>状态</th>
            <th>版本</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(trade, index) in store.trades"
            :key="trade.id"
            data-testid="trade-row"
            :data-trade-id="trade.id"
          >
            <td>{{ trade.tradeType }}</td>
            <td>{{ trade.side }}</td>
            <td>{{ trade.underlying }}</td>
            <td>{{ trade.quantity }}</td>
            <td data-testid="trade-status">{{ trade.status }}</td>
            <td>{{ trade.version }}</td>
            <td>
              <button
                type="button"
                data-testid="submit-trade"
                :disabled="trade.status !== 'DRAFT'"
                @click="submitTrade(index)"
              >
                提交
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
