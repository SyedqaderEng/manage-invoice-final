export function mergePage<T extends { id: string }>(existing: T[], incoming: T[]): T[] {
  const seen = new Set(existing.map(item => item.id))
  return [...existing, ...incoming.filter(item => {
    if (seen.has(item.id)) return false
    seen.add(item.id)
    return true
  })]
}
