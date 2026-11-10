interface AvatarProps {
  name: string
}

export function Avatar({ name }: AvatarProps) {
  const initials = name.split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0]).join('').toUpperCase()
  return <span className="avatar" aria-label={name}>{initials || '?'}</span>
}
