import { ref } from 'vue'
import { defineStore } from 'pinia'
import { apiFetch } from '../api/client'
import type { CreateTrade, Trade } from '../types'

export const useTradesStore = defineStore('trades', () => {
  const trades = ref<Trade[]>([])
  const loading = ref(false)

  async function load(portfolioCode = 'ALPHA') {
    loading.value = true
    try {
      trades.value = await apiFetch<Trade[]>(
        `/trades?portfolioCode=${encodeURIComponent(portfolioCode)}`,
      )
    } finally {
      loading.value = false
    }
  }

  async function create(request: CreateTrade, idempotencyKey: string) {
    const trade = await apiFetch<Trade>('/trades', {
      method: 'POST',
      headers: { 'Idempotency-Key': idempotencyKey },
      body: JSON.stringify(request),
    })
    const index = trades.value.findIndex((item) => item.id === trade.id)
    if (index >= 0) trades.value[index] = trade
    else trades.value.unshift(trade)
    return trade
  }

  async function submit(trade: Trade) {
    return replace(await apiFetch<Trade>(`/trades/${trade.id}/submit`, {
      method: 'POST',
      body: JSON.stringify({ expectedVersion: trade.version }),
    }))
  }

  async function approve(trade: Trade, comment: string) {
    return replace(await apiFetch<Trade>(`/trades/${trade.id}/approve`, {
      method: 'POST',
      body: JSON.stringify({ expectedVersion: trade.version, comment }),
    }))
  }

  async function reject(trade: Trade, comment: string) {
    return replace(await apiFetch<Trade>(`/trades/${trade.id}/reject`, {
      method: 'POST',
      body: JSON.stringify({ expectedVersion: trade.version, comment }),
    }))
  }

  function replace(trade: Trade) {
    const index = trades.value.findIndex((item) => item.id === trade.id)
    if (index >= 0) trades.value[index] = trade
    return trade
  }

  return { trades, loading, load, create, submit, approve, reject }
})
