<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ApiClientError } from '../api/client'
import { useTradesStore } from '../stores/trades'
import type { Trade } from '../types'

const store = useTradesStore()
const error = ref('')
const message = ref('')
const comments = reactive<Record<string, string>>({})
const submitted = computed(() => store.trades.filter((trade) => trade.status === 'SUBMITTED'))

onMounted(refresh)

async function refresh() {
  error.value = ''
  try {
    await store.load('ALPHA')
  } catch (reason) {
    showError(reason)
  }
}

async function approve(trade: Trade) {
  error.value = ''
  try {
    await store.approve(trade, comments[trade.id] ?? '')
    message.value = `已批准 ${trade.id}`
  } catch (reason) {
    showError(reason)
  }
}

async function reject(trade: Trade) {
  error.value = ''
  try {
    await store.reject(trade, comments[trade.id] ?? '')
    message.value = `已拒绝 ${trade.id}`
  } catch (reason) {
    showError(reason)
  }
}

function showError(reason: unknown) {
  error.value = reason instanceof ApiClientError ? `${reason.body.code}: ${reason.message}` : String(reason)
}
</script>

<template>
  <section aria-labelledby="approval-title">
    <div class="actions">
      <h2 id="approval-title">审批台</h2>
      <button class="secondary" type="button" data-testid="refresh-approvals" @click="refresh">
        刷新
      </button>
    </div>
    <p v-if="error" class="error" role="alert" data-testid="approval-error">{{ error }}</p>
    <p v-if="message" class="success" role="status" data-testid="approval-message">{{ message }}</p>

    <p v-if="submitted.length === 0" class="panel" data-testid="no-submitted-trades">
      当前没有待审批交易。
    </p>
    <article
      v-for="trade in submitted"
      :key="trade.id"
      class="panel"
      data-testid="approval-row"
      :data-trade-id="trade.id"
    >
      <h3>{{ trade.tradeType }} / {{ trade.side }} / {{ trade.underlying }}</h3>
      <p>组合 {{ trade.portfolioCode }}，数量 {{ trade.quantity }}，执行价 {{ trade.strike }}</p>
      <label :for="`comment-${trade.id}`">审批意见</label>
      <textarea
        :id="`comment-${trade.id}`"
        v-model="comments[trade.id]"
        data-testid="approval-comment"
        maxlength="500"
      />
      <div class="actions">
        <button type="button" data-testid="approve-trade" @click="approve(trade)">批准</button>
        <button
          class="danger"
          type="button"
          data-testid="reject-trade"
          @click="reject(trade)"
        >
          拒绝
        </button>
      </div>
    </article>
  </section>
</template>
