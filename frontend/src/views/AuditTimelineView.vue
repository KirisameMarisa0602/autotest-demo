<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiClientError, apiFetch } from '../api/client'
import type { AuditEvent } from '../types'

const events = ref<AuditEvent[]>([])
const error = ref('')

onMounted(load)

async function load() {
  error.value = ''
  try {
    events.value = await apiFetch<AuditEvent[]>('/audit-events')
  } catch (reason) {
    error.value = reason instanceof ApiClientError
      ? `${reason.body.code}: ${reason.message}`
      : String(reason)
  }
}
</script>

<template>
  <section aria-labelledby="audit-title">
    <div class="actions">
      <h2 id="audit-title">审计时间线</h2>
      <button class="secondary" type="button" data-testid="refresh-audit" @click="load">
        刷新
      </button>
    </div>
    <p v-if="error" class="error" role="alert" data-testid="audit-error">{{ error }}</p>
    <ol class="panel" data-testid="audit-timeline">
      <li
        v-for="event in events"
        :key="event.id"
        data-testid="audit-event"
        :data-event-type="event.eventType"
      >
        <strong>{{ event.eventType }}</strong>
        <time :datetime="event.occurredAt">{{ event.occurredAt }}</time>
        <span> actor={{ event.actor }}; {{ event.details }}</span>
      </li>
    </ol>
  </section>
</template>
