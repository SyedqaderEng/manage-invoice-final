export function formatPlanPrice(amountInCents: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amountInCents / 100)
}
