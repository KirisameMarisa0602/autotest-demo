import { ref } from 'vue'
import { defineStore } from 'pinia'
import { apiFetch } from '../api/client'
import type { CreateTrade, Trade, TradePage } from '../types'

export const useTradesStore = defineStore('trades', () => {
  const trades = ref<Trade[]>([])
  const loading = ref(false)
  const totalItems = ref(0)

  async function load(portfolioCode = 'ALPHA', status = '') {
    loading.value = true
    try {
      const query = new URLSearchParams({ portfolioCode, size: '100' })
      if (status) query.set('status', status)
      const page = await apiFetch<TradePage>(`/trades?${query.toString()}`)
      trades.value = page.items
      totalItems.value = page.totalItems
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

  return { trades, loading, totalItems, load, create, submit, approve, reject }
})
