interface StatusBadgeProps {
  label: string
  tone?: 'neutral' | 'success' | 'warning' | 'danger'
}

export function StatusBadge({ label, tone = 'neutral' }: StatusBadgeProps) {
  return <span className={`status-badge status-${tone}`}><span aria-hidden="true" />{label}</span>
}
