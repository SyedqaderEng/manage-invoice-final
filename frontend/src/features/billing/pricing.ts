export function formatPlanPrice(amountInCents: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amountInCents / 100)
}

export interface PersonalPlan {
  id: 'free' | 'pro'
  name: string
  monthlyCents: number
  yearlyCents: number
  currency: 'usd'
  lifetimeFreeReceipts: number | null
  unlimitedReceipts: boolean
  trialMonths: number
  checkoutAvailable: boolean
}

export function parsePersonalPlans(value: unknown): PersonalPlan[] {
  if (!Array.isArray(value) || value.length !== 2) throw new Error('The plan catalog is unavailable.')
  const isPlan = (item: unknown): item is PersonalPlan => {
    if (!item || typeof item !== 'object') return false
    const plan = item as Record<string, unknown>
    return (plan.id === 'free' || plan.id === 'pro') && typeof plan.name === 'string'
      && typeof plan.monthlyCents === 'number' && Number.isSafeInteger(plan.monthlyCents) && plan.monthlyCents >= 0
      && typeof plan.yearlyCents === 'number' && Number.isSafeInteger(plan.yearlyCents) && plan.yearlyCents >= 0
      && plan.currency === 'usd' && (plan.lifetimeFreeReceipts === null || (typeof plan.lifetimeFreeReceipts === 'number' && Number.isSafeInteger(plan.lifetimeFreeReceipts) && plan.lifetimeFreeReceipts >= 0))
      && typeof plan.unlimitedReceipts === 'boolean' && typeof plan.trialMonths === 'number'
      && Number.isSafeInteger(plan.trialMonths) && plan.trialMonths >= 0 && typeof plan.checkoutAvailable === 'boolean'
  }
  if (!value.every(isPlan) || new Set(value.map(plan => plan.id)).size !== 2) throw new Error('The plan catalog is unavailable.')
  return value
}
