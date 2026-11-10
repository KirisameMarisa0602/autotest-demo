export type Role = 'TRADER' | 'RISK_MANAGER' | 'APPROVER'
export type TradeType = 'EUROPEAN_OPTION' | 'FORWARD'
export type Side = 'LONG' | 'SHORT'
export type OptionType = 'CALL' | 'PUT'
export type TradeStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED'

export interface Trade {
  id: string
  portfolioCode: string
  tradeType: TradeType
  side: Side
  underlying: string
  quantity: number
  strike: number
  maturity: string
  optionType?: OptionType
  status: TradeStatus
  version: number
  createdAt: string
  updatedAt: string
  createdBy: string
  decisionBy?: string
  decisionComment?: string
}

export interface CreateTrade {
  portfolioCode: string
  tradeType: TradeType
  side: Side
  underlying: string
  quantity: number
  strike: number
  maturity: string
  optionType?: OptionType
}

export interface RiskScenario {
  key: string
  spotShockPct: number
  stressedPresentValue: number
  pnl: number
}

export interface RiskReport {
  portfolioCode: string
  approvedTradeCount: number
  presentValue: number
  delta: number
  currency: string
  scenarios: RiskScenario[]
}

export interface AuditEvent {
  id: string
  eventType: string
  aggregateType: string
  aggregateId: string
  actor: string
  occurredAt: string
  details: string
}
